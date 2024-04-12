package example.spring.boot.domains.books.persistence

import example.spring.boot.MongoDBInitializer
import org.springframework.test.context.ContextConfiguration
import kotlin.annotation.AnnotationTarget.CLASS

@Retention
@Target(CLASS)
@ContextConfiguration(initializers = [MongoDBInitializer::class])
annotation class InitializeWithContainerizedMongoDB
