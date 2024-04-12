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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.util.UUID.randomUUID

@Import(WebSecurityConfiguration::class)
@MockkBean(BookCollection::class, BookCoverService::class, Clock::class)
@WebMvcTest(BooksController::class)
class BooksControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val bookCollection: BookCollection,
) {

    @Nested
    @DisplayName("Get Book By Id")
    inner class GetBookById {


        @Test
        @WithMockUser(authorities = [Authorities.SCOPE_API, Authorities.ROLE_USER])
        fun `returns book when it exists`() {
            val id = randomUUID()

            val book = Book(
                id = id,
                data = BookData(
                    isbn = Isbn("1234567890"),
                    title = Title("The Lord of the Rings")
                ),
            )
            every { bookCollection.getBook(id) } returns book

            mvc.get("/api/books/{id}", id)
                .andExpect {
                    status { isOk() }
                    content {
                        json(
                            """ 
                        {
                          "id": "$id",
                          "available": true,
                          "data": {
                            "isbn": "${book.data.isbn}",
                            "title": "${book.data.title}",
                            "authors": []
                          }
                        }    
                        """.trimIndent(),
                            strict = true,
                        )
                    }
                }
        }

        @Test
        @WithMockUser(authorities = [Authorities.SCOPE_API, Authorities.ROLE_USER])
        fun `returns 404 NOT FOUND when the book does not exist`() {
            val id = randomUUID()
            every { bookCollection.getBook(id) } returns null
            mvc.get("/api/books/{id}", id)
                .andExpect {
                    status { isNotFound() }
                    content {
                        string("")
                    }
                }
        }
    }

    @Nested
    @DisplayName("Add Book Id")
    inner class AddBook {
        @Test
        @WithMockUser(authorities = [Authorities.SCOPE_API, Authorities.ROLE_USER])
        fun `returns 400 bad request when request is not valid`() {
            val body = "$$$$$$$$$$$$$$$$$"
            mvc.post("/api/books") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isBadRequest() }
                content {
                    string("")
                }
            }
        }
    }
}

