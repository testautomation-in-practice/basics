package example.spring.boot.domains.books.api

import arrow.core.left
import arrow.core.right
import com.ninjasquad.springmockk.MockkBean
import example.spring.boot.common.security.Authorities.ROLE_CURATOR
import example.spring.boot.common.security.Authorities.SCOPE_API
import example.spring.boot.common.security.WebSecurityConfiguration
import example.spring.boot.domains.books.business.BookCollection
import example.spring.boot.domains.books.business.BookNotFound
import example.spring.boot.domains.books.business.BookUpdateFailed
import example.spring.boot.domains.books.model.*
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.*
import java.util.UUID.*

@Import(WebSecurityConfiguration::class)
@MockkBean(BookCollection::class)
@WebMvcTest(BooksController::class)
internal class BooksControllerIntegrationTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val bookCollection: BookCollection,
) {

    @Nested
    @DisplayName("Add Book")
    inner class AddBook {

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `creating a book returns its representation with status 201 CREATED`() {
            every { bookCollection.addBook(any()) } returns Books.LORD_OF_THE_RINGS_UNENRICHED

            mockMvc.post("/api/books") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "isbn": "${BooksData.LORD_OF_THE_RINGS.isbn}",
                    "title": "${BooksData.LORD_OF_THE_RINGS.title}"
                }
                """.trimIndent()
            }.andExpect {
                status { isCreated() }
                content {
                    contentType(APPLICATION_JSON)
                    json(
                        """
                        {
                            "id": "${Ids.LORD_OF_THE_RINGS}",
                            "data": {
                                "isbn": "${BooksData.LORD_OF_THE_RINGS.isbn}",
                                "title": "${BooksData.LORD_OF_THE_RINGS.title}",
                                "authors": []
                            },
                            "available": true
                        }
                        """.trimIndent(),
                        strict = true
                    )
                }
            }
        }

        @Test
        fun `creating a book without authorization returns an error and status 403 UNAUTHORIZED`() {
            mockMvc.post("/api/books") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "isbn": "1234567890",
                    "title": "Lord of the Rings"
                }
                """.trimIndent()
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `creating a book with invalid input returns an error and status 400 BAD REQUEST`() {
            mockMvc.post("/api/books") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "isbn": "1234567890X",
                    "title": "Lord of the Rings"
                }
                """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("Get Book By Id")
    inner class GetBookById {

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `getting a book that does not exist returns nothing and status 404 NOT FOUND `() {
            every { bookCollection.getBook(Ids.LORD_OF_THE_RINGS) } returns null

            mockMvc.get("/api/books/${Ids.LORD_OF_THE_RINGS}")
                .andExpect {
                    status { isNotFound() }
                }
        }

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `getting a book that exists returns its representation and status 200 OK`() {
            every { bookCollection.getBook(Ids.LORD_OF_THE_RINGS) } returns Books.LORD_OF_THE_RINGS

            mockMvc.get("/api/books/${Ids.LORD_OF_THE_RINGS}")
                .andExpect {
                    status { isOk() }
                    content {
                        contentType(APPLICATION_JSON)
                        json(
                            """
                            {
                                "id": "${Ids.LORD_OF_THE_RINGS}",
                                "data": {
                                    "title": "${BooksData.LORD_OF_THE_RINGS.title}",
                                    "numberOfPages": ${BooksData.LORD_OF_THE_RINGS.numberOfPages!!.toInt()},
                                    "isbn": "${BooksData.LORD_OF_THE_RINGS.isbn}",
                                    "authors": ["${BooksData.LORD_OF_THE_RINGS.authors.first()}"]
                                },
                                "available": true
                            }
                            """.trimIndent(),
                            strict = true
                        )
                    }
                }
        }

        @Test
        @WithMockUser(authorities = [ROLE_CURATOR, SCOPE_API])
        fun `returned book data includes borrowed status if user is curator`() {
            every { bookCollection.getBook(Ids.LORD_OF_THE_RINGS) } returns Books.LORD_OF_THE_RINGS_BORROWED

            mockMvc.get("/api/books/${Ids.LORD_OF_THE_RINGS}")
                .andExpect {
                    status { isOk() }
                    content {
                        contentType(APPLICATION_JSON)
                        json(
                            """
                            {
                                "id": "${Ids.LORD_OF_THE_RINGS}",
                                "data": {
                                    "title": "${BooksData.LORD_OF_THE_RINGS.title}",
                                    "numberOfPages": ${BooksData.LORD_OF_THE_RINGS.numberOfPages!!.toInt()},
                                    "isbn": "${BooksData.LORD_OF_THE_RINGS.isbn}",
                                    "authors": ["${BooksData.LORD_OF_THE_RINGS.authors.first()}"]
                                },
                                "available": false,
                                "borrowed": {
                                    "by": "Frodo",
                                    "at": "2023-05-05T12:34:56.789Z"
                                }
                            }
                            """.trimIndent(),
                            strict = true
                        )
                    }
                }
        }

        @Test
        fun `getting a book without authorization returns an error and status 403 UNAUTHORIZED`() {
            mockMvc.get("/api/books/${Ids.LORD_OF_THE_RINGS}")
                .andExpect {
                    status { isUnauthorized() }
                }
        }


    }

    @Nested
    @DisplayName("Borrow Book By Id")
    inner class BorrowBookById {

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `successfully borrowing returns book data and status 200 OK`() {

            every {
                bookCollection.borrowBook(any(), any())
            } returns Books.LORD_OF_THE_RINGS_BORROWED.right()

            mockMvc.post("/api/books/${Ids.LORD_OF_THE_RINGS}/borrow") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "borrower": "Frodo"
                }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                content {
                    contentType(APPLICATION_JSON)
                    json(
                        """
                        {
                            "id": "${Ids.LORD_OF_THE_RINGS}",
                            "data": {
                                "isbn": "${BooksData.LORD_OF_THE_RINGS.isbn}",
                                "title": "${BooksData.LORD_OF_THE_RINGS.title}",
                                "numberOfPages": ${BooksData.LORD_OF_THE_RINGS.numberOfPages},
                                "authors": ["${BooksData.LORD_OF_THE_RINGS.authors.first()}"]
                            },
                            "available": false
                        }
                        """.trimIndent(),
                        strict = true
                    )
                }
            }
        }

        @Test
        @WithMockUser(authorities = [SCOPE_API, ROLE_CURATOR])
        fun `successfully borrowing returns book data witb borrowed status if user is curator`() {

            every {
                bookCollection.borrowBook(any(), any())
            } returns Books.LORD_OF_THE_RINGS_BORROWED.right()

            mockMvc.post("/api/books/${Ids.LORD_OF_THE_RINGS}/borrow") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "borrower": "Frodo"
                }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                content {
                    contentType(APPLICATION_JSON)
                    json(
                        """
                        {
                            "id": "${Ids.LORD_OF_THE_RINGS}",
                            "data": {
                                "isbn": "${BooksData.LORD_OF_THE_RINGS.isbn}",
                                "title": "${BooksData.LORD_OF_THE_RINGS.title}",
                                "numberOfPages": ${BooksData.LORD_OF_THE_RINGS.numberOfPages},
                                "authors": ["${BooksData.LORD_OF_THE_RINGS.authors.first()}"]
                            },
                            "borrowed": {
                                "by": "Frodo",
                                "at": "2023-05-05T12:34:56.789Z"
                            },
                            "available": false
                        }
                        """.trimIndent(),
                        strict = true
                    )
                }
            }

        }

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `returns error and status 404 NOT FOUND if book does not exist`() {

            every {
                bookCollection.borrowBook(any(), any())
            } returns BookNotFound.left()

            mockMvc.post("/api/books/${Ids.LORD_OF_THE_RINGS}/borrow") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "borrower": "Frodo"
                }
                """.trimIndent()
            }.andExpect {
                status { isNotFound() }
            }
        }

        @Test
        @WithMockUser(authorities = [SCOPE_API])
        fun `returns error and status 409 CONFLICT if book is already borrowed`() {

            every {
                bookCollection.borrowBook(any(), any())
            } returns BookUpdateFailed.left()

            mockMvc.post("/api/books/${Ids.LORD_OF_THE_RINGS}/borrow") {
                contentType = APPLICATION_JSON
                content = """
                {
                    "borrower": "Frodo"
                }
                """.trimIndent()
            }.andExpect {
                status { isConflict() }
            }
        }
    }

}