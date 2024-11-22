package example.spring.boot.domains.books.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import example.spring.boot.Examples
import example.spring.boot.MongoDBInitializer
import example.spring.boot.S3Initializer
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.*

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [S3Initializer::class, MongoDBInitializer::class])
class BooksApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val mapper: ObjectMapper,
) {
    private val rest: TestRestTemplate = TestRestTemplate(RestTemplateBuilder().rootUri("http://localhost:$port"))

    @Test
    fun setup() {
        RestAssured.port = port
    }

    @Test
    fun `books api test`() {
        val bookId = Given {
            auth().basic("curator", "curator".reversed())
            header("Content-Type", "application/json")
            body(
                """
                {
                  "isbn": "1234567890",
                  "title": "Clean Code"
                }
                """.trimIndent()
            )
        } When {
            post("/api/books")
        } Then {
            statusCode(CREATED.value())
        } Extract {
            val response = mapper.readValue<BookRepresentation>(body().asInputStream())

            response.data.title shouldBe Title("Clean Code")
            response.data.isbn shouldBe Isbn("1234567890")
            response.data.authors shouldHaveSize 0
            response.data.numberOfPages shouldBe null
            response.available shouldBe true
            response.borrowed shouldBe null

            response.id
        }

        Given {
            auth().basic("user", "user".reversed())
            header("Content-Type", "application/json")
            body(
                """
                {
                  "borrower": "Gandalf"
                }
                """.trimIndent()
            )
        } When {
            post("/api/books/$bookId/borrow")
        } Then {
            statusCode(OK.value())
        } Extract {
            val response = mapper.readValue<BookRepresentation>(body().asInputStream())

            response.data.title shouldBe Title("Clean Code")
            response.data.isbn shouldBe Isbn("1234567890")
            response.data.authors shouldHaveSize 0
            response.data.numberOfPages shouldBe null
            response.available shouldBe false
            response.borrowed shouldBe null

            response.id
        }
    }

    @Nested
    inner class CreateBook {

        @Test
        fun `creates Book correctly`() {
            val requestEntity = HttpEntity(
                AddBookRequest(Examples.CLEAN_CODE.isbn, Examples.CLEAN_CODE.title),
                curatorBasicAuthHeader()
            )

            val exchange = rest.exchange("/api/books", HttpMethod.POST, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(CREATED)

            val body = ObjectMapper().findAndRegisterModules()
                .readValue(exchange.body, BookRepresentation::class.java)

            assertThat(body.available).isTrue()
            assertThat(body.borrowed).isNull()
            assertThat(body.data).isEqualTo(Examples.CLEAN_CODE)
        }

        @Test
        fun `creating Book fails when caller has insufficient rights`() {
            val requestEntity = HttpEntity(
                AddBookRequest(Examples.CLEAN_CODE.isbn, Examples.CLEAN_CODE.title),
                userBasicAuthHeader()
            )

            val exchange = rest.exchange("/api/books", HttpMethod.POST, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(FORBIDDEN)
            val body = ObjectMapper().findAndRegisterModules().readTree(exchange.body)

            assertThat(body.get("timestamp")).isNotNull()
            assertThat(body.get("status")?.asText()).isEqualTo("403")
            assertThat(body.get("error")?.asText()).isEqualTo("Forbidden")
            assertThat(body.get("path")?.asText()).isEqualTo("/api/books")
        }

        @Test
        fun `creating Book fails when given Isbn is invalid`() {
            val headers = curatorBasicAuthHeader().apply {
                set(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            }
            val requestEntity = HttpEntity("{\"title\": \"Clean Code\", \"isbn\": \"978-01322350884\"}", headers)

            val exchange = rest.exchange("/api/books", HttpMethod.POST, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(BAD_REQUEST)
            val body = ObjectMapper().findAndRegisterModules().readTree(exchange.body)

            assertThat(body.get("timestamp")).isNotNull()
            assertThat(body.get("status")?.asText()).isEqualTo("400")
            assertThat(body.get("error")?.asText()).isEqualTo("Bad Request")
            assertThat(body.get("path")?.asText()).isEqualTo("/api/books")
        }
    }

    @Nested
    inner class DeleteBook {

        @Test
        fun `deletes Book successfully when it exists`() {
            val requestEntity = HttpEntity<Unit>(curatorBasicAuthHeader())

            val exchange =
                rest.exchange("/api/books/${UUID.randomUUID()}", HttpMethod.DELETE, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(NO_CONTENT)
            assertThat(exchange.body).isNull()
        }

        @Test
        fun `deletes Book successfully when it does not exist`() {
            val requestEntity = HttpEntity<Unit>(curatorBasicAuthHeader())

            val exchange =
                rest.exchange("/api/books/${UUID.randomUUID()}", HttpMethod.DELETE, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(NO_CONTENT)
            assertThat(exchange.body).isNull()
        }

    }

    fun curatorBasicAuthHeader() =
        HttpHeaders().apply {
            set(
                HttpHeaders.AUTHORIZATION,
                "Basic ${Base64.getEncoder().encodeToString("curator:${"curator".reversed()}".toByteArray())}"
            )
        }

    fun userBasicAuthHeader() =
        HttpHeaders().apply {
            set(
                HttpHeaders.AUTHORIZATION,
                "Basic ${Base64.getEncoder().encodeToString("user:${"user".reversed()}".toByteArray())}"
            )
        }

}