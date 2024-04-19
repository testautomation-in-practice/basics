package example.spring.boot.domains.books

import example.spring.boot.Examples
import example.spring.boot.MongoDBInitializer
import example.spring.boot.S3Initializer
import example.spring.boot.domains.books.api.AddBookRequest
import example.spring.boot.domains.books.api.BookRepresentation
import example.spring.boot.domains.books.api.BorrowBookRequest
import example.spring.boot.domains.books.model.primitives.Borrower
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(
    initializers = [
        S3Initializer::class,
        MongoDBInitializer::class,
    ]
)
class BookApplicationTest {

    @BeforeEach
    fun setup(@LocalServerPort port: Int) {
        RestAssured.port = port
    }

    @Test
    fun `system end`() {
        val id = Given {
            body(AddBookRequest(Examples.CLEAN_CODE.isbn, Examples.CLEAN_CODE.title))
            header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            auth().basic("curator", "rotaruc")
        } When {
            post("/api/books")
        } Then {
            statusCode(CREATED.value())
        } Extract {
            this.body().`as`(BookRepresentation::class.java).id
        }

        Given {
            body(BorrowBookRequest(Borrower("gandalf")))
            header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            auth().basic("user", "resu")
        } When {
            post("/api/books/$id/borrow")
        } Then {
            statusCode(OK.value())
        }

        Given {
            body(BorrowBookRequest(Borrower("gandalf")))
            header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            auth().basic("user", "resu")
        } When {
            post("/api/books/$id/borrow")
        } Then {
            statusCode(HttpStatus.CONFLICT.value())
        }

        Given {
            auth().basic("user", "resu")
        } When {
            post("/api/books/$id/return")
        } Then {
            statusCode(OK.value())
        }

        Given {
            auth().basic("curator", "rotaruc")
        } When {
            delete("/api/books/$id")
        } Then {
            statusCode(HttpStatus.NO_CONTENT.value())
        }
    }
}