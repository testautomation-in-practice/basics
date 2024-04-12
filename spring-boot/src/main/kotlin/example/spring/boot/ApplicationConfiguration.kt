package example.spring.boot

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.util.IdGenerator
import org.springframework.util.JdkIdGenerator
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.time.Clock
import java.util.*
import java.util.function.Supplier

@EnableAsync
@EnableCaching
@Configuration
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class ApplicationConfiguration(private val s3: AmazonS3) {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun idGenerator(): IdGenerator = JdkIdGenerator()

    @Bean
    fun cacheManager(caches: List<Cache>): CacheManager =
        SimpleCacheManager().apply { setCaches(caches) }

    @EventListener
    fun prepareBucket(event: ApplicationStartedEvent) {
        s3.createBucket("bucket")

        val fellowshipCover = File(this::class.java.getResource("/bookcovers/fellowship_of_the_ring.jpg")!!.path)
        s3.putObject("bucket", "d0b64031-17e6-4595-a3af-dcee894794f2", fellowshipCover)
    }
}

// In a real application, these initializers would not be part of the production code.
// We do it like this for the example application because it makes it easier to start a local instance.
// Usually these would be on the test classpath and only used during testing.

class S3Initializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) {
        val container = S3Container().withServices(LocalStackContainer.Service.S3).apply { start() }
        createS3Bean(container, applicationContext)
    }

    private fun createS3Bean(
        container: LocalStackContainer,
        applicationContext: GenericApplicationContext
    ) {
        applicationContext.registerBean(
            AmazonS3::class.java.simpleName,
            AmazonS3::class.java,
            Supplier {
                AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(
                        EndpointConfiguration(
                            container.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                            container.region
                        )
                    )
                    .withCredentials(
                        AWSStaticCredentialsProvider(BasicAWSCredentials(container.accessKey, container.secretKey))
                    )
                    .build()
            }
        )
    }

    private class S3Container : LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
}

class MongoDBInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        private val container: MongoDBContainer by lazy {
            MongoDBContainer("mongo:6.0.6").apply { start() }
        }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val database = randomDatabaseName()

        val props = Properties().also {
            it["spring.data.mongodb.host"] = container.host
            it["spring.data.mongodb.port"] = container.firstMappedPort
            it["spring.data.mongodb.database"] = database
        }
        applicationContext.environment.propertySources
            .addFirst(PropertiesPropertySource("MongoProperties", props))
    }

    private fun randomDatabaseName() = "test_${UUID.randomUUID()}".replace("-", "")

}
