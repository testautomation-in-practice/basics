package example.spring.boot.domains.books

import example.spring.boot.MongoDBInitializer
import example.spring.boot.S3Initializer
import example.spring.boot.domains.books.api.BorrowBookRequest
import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.primitives.Borrower
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import example.spring.boot.domains.books.persistence.MongoDbBookRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType.JSON
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.ContextConfiguration
import java.util.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [MongoDBInitializer::class, S3Initializer::class])
class EndToEndTest @Autowired constructor(
    private val repo: MongoDbBookRepository,
) {

    @BeforeEach
    fun setup(@LocalServerPort port: Int) {
        RestAssured.port = port
    }

    @Test
    fun `user can borrow and return a book`() {
        val id = UUID.randomUUID()
        val book = Book(
            id = id,
            data = BookData(
                isbn = Isbn("1234567890"),
                title = Title("Lod of the Rings"),
            ),
        )
        repo.insert(book)

        Given {
            auth().basic("user", "resu")
            contentType(JSON)
            body(BorrowBookRequest(Borrower("Gandalf")))
        } When {
            post("/api/books/$id/borrow")
        } Then {
            statusCode(OK.value())
        }

        Given {
            auth().basic("user", "resu")
            contentType(JSON)
            body(BorrowBookRequest(Borrower("Gandalf")))
        } When {
            post("/api/books/$id/borrow")
        } Then {
            statusCode(CONFLICT.value())
        }
    }

}
