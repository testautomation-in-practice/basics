package example.spring.boot.domains.books

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import example.spring.boot.MongoDBInitializer
import example.spring.boot.S3Initializer
import example.spring.boot.domains.books.api.BookRepresentation
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured
import io.restassured.http.ContentType.JSON
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.ContextConfiguration
import java.util.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(
    initializers = [
        S3Initializer::class,
        MongoDBInitializer::class,
    ]
)
@TestMethodOrder(OrderAnnotation::class) // only if you are really certain you need a static order
class BooksEndToEndTest(
    @LocalServerPort port: Int,
    private val mapper: ObjectMapper,
) {

    init {
        RestAssured.port = port
    }

    private val bookData = BookData(
        isbn = Isbn("1234567890"),
        title = Title("The Lord of the Rings")
    )

    @Test
    @Order(1)
    fun `end to end test`() {
        val id = createBook()
        borrowBook(id)
        borrowBook(id, expectedStatus = CONFLICT)
    }

    private fun borrowBook(id: UUID, expectedStatus: HttpStatus = OK) {
        TODO("Not yet implemented")
    }

    private fun createBook(): UUID {
        @Language("JSON")
        val createBookRequest = """
                    {
                      "isbn": "${bookData.isbn}",
                      "title": "${bookData.title}"
                    }
                    """

        val response: BookRepresentation = Given {
            contentType(JSON)
            body(createBookRequest)
            auth().basic("curator", "curator".reversed())
        } When {
            post("/api/books")
        } Then {
            statusCode(HttpStatus.CREATED.value())
        } Extract {
            mapper.readValue<BookRepresentation>(body().asString())
        }

        response.available shouldBe true
        response.borrowed shouldBe null
        response.data

        return response.id
    }
}
