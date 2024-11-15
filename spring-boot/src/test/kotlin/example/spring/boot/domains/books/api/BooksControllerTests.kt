package example.spring.boot.domains.books.api

import com.ninjasquad.springmockk.MockkBean
import example.spring.boot.common.security.Authorities
import example.spring.boot.common.security.WebSecurityConfiguration
import example.spring.boot.domains.books.business.BookCollection
import example.spring.boot.domains.books.business.BookCoverService
import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.util.UUID

/*

Included:
    - Spring WebMVC
    - Spring Security
    - Our Spring Security Configurations
    - Our BooksController
    - Mocked Dependencies: BookCollection, BookCoverService, Clock

Not Included:
    - about a million different Spring Beans
    - event publishing
    - database stuff

 */

@WebMvcTest(BooksController::class)
@Import(WebSecurityConfiguration::class)
@MockkBean(BookCollection::class, BookCoverService::class, Clock::class)
@WithMockUser(authorities = [Authorities.SCOPE_API, Authorities.ROLE_USER])
class BooksControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val collection: BookCollection,
) {

    @Test
    fun `adding a new book returns its representation`() {
        val bookId = UUID.fromString("7f259300-8feb-4b31-84d1-e09e8630cf24")
        val bookData = BookData(Isbn("1234567890"), Title("Test Book"))
        every { collection.addBook(bookData) } returns Book(bookId, bookData)

        mockMvc.post("/api/books") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "isbn": "1234567890",
                  "title": "Test Book"
                }
                """
        }.andExpect {
            status { isCreated() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(
                    """
                    {
                      "id": "7f259300-8feb-4b31-84d1-e09e8630cf24",
                      "timestamp": "2024-11-15T12:34:56.789Z",
                      "data": {
                        "isbn": "1234567890",
                        "title": "Test Book…· iu",
                        "authors": []
                      },
                      "available": true
                    }
                    """,
                    strict = true,
                )
            }
        }
    }
}
