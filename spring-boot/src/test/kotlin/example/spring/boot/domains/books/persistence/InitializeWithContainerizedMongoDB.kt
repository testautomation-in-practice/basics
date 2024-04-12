package example.spring.boot.domains.books.persistence

import example.spring.boot.MongoDBInitializer
import example.spring.boot.S3Initializer
import org.springframework.test.context.ContextConfiguration
import kotlin.annotation.AnnotationTarget.CLASS

@Retention
@Target(CLASS)
@ContextConfiguration(initializers = [MongoDBInitializer::class])
annotation class InitializeWithContainerizedMongoDB

@Retention
@Target(CLASS)
@ContextConfiguration(initializers = [S3Initializer::class])
annotation class InitializeWithContainerizedS3
