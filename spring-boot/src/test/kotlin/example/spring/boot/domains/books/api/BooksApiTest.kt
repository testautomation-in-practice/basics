package example.spring.boot.domains.books.api

import com.fasterxml.jackson.databind.ObjectMapper
import example.spring.boot.Examples
import example.spring.boot.helper.SpringRestTest
import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import java.util.*

class BooksApiTest : SpringRestTest() {

    @Nested
    inner class CreateBook {

        @Test
        fun `creates Book correctly`() {
            val requestEntity = HttpEntity(
                AddBookRequest(Examples.CLEAN_CODE.isbn, Examples.CLEAN_CODE.title),
                curatorBasicAuthHeader()
            )

            every { bookRepository.insert(any()) } just Runs
            every { bookEventPublisher.publish(any()) } just Runs

            val exchange = rest.exchange(
                "/api/books",
                HttpMethod.POST,
                requestEntity,
                String::class.java
            )

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

            every { bookRepository.insert(any()) } just Runs
            every { bookEventPublisher.publish(any()) } just Runs

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

            every { bookRepository.insert(any()) } just Runs
            every { bookEventPublisher.publish(any()) } just Runs

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

            every { bookRepository.delete(any()) } returns true
            every { bookEventPublisher.publish(any()) } just Runs

            val exchange =
                rest.exchange("/api/books/${UUID.randomUUID()}", HttpMethod.DELETE, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(NO_CONTENT)
            assertThat(exchange.body).isNull()

            verify(exactly = 1) { bookEventPublisher.publish(any()) }
        }

        @Test
        fun `deletes Book successfully when it does not exist`() {
            val requestEntity = HttpEntity<Unit>(curatorBasicAuthHeader())

            every { bookRepository.delete(any()) } returns false
            every { bookEventPublisher.publish(any()) } just Runs

            val exchange =
                rest.exchange("/api/books/${UUID.randomUUID()}", HttpMethod.DELETE, requestEntity, String::class.java)

            assertThat(exchange.statusCode).isEqualTo(NO_CONTENT)
            assertThat(exchange.body).isNull()

            verify { bookEventPublisher wasNot called }
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