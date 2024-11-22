package example.spring.boot.domains.books.business.bookcollectiontest

import arrow.core.Either
import example.spring.boot.domains.books.business.BookCollection
import example.spring.boot.domains.books.business.BookEventPublisher
import example.spring.boot.domains.books.business.BookFailure
import example.spring.boot.domains.books.business.BookNotFound
import example.spring.boot.domains.books.business.BookRepository
import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.State
import example.spring.boot.domains.books.model.events.BookBorrowedEvent
import example.spring.boot.domains.books.model.primitives.Author
import example.spring.boot.domains.books.model.primitives.Borrower
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.NumberOfPages
import example.spring.boot.domains.books.model.primitives.Title
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.util.IdGenerator
import java.time.Clock
import java.time.Instant
import java.util.UUID.randomUUID

internal class BookCollectionTest {

    private val idGenerator = mockk<IdGenerator>()
    private val repository = mockk<BookRepository>()
    private val eventPublisher = mockk<BookEventPublisher>()
    private val clock = mockk<Clock>()
    private val cut = BookCollection(repository, eventPublisher, idGenerator, clock)

    private val bookId = randomUUID()
    private val borrower = Borrower("Gandalf")
    private val book = Book(
        id = bookId,
        data = BookData(
            Isbn("1234567890"), Title("Lord of the Rings"),
            NumberOfPages(123), setOf(Author("JRR Tolkien"))
        )
    )

    @BeforeEach
    fun setup() {
        println("Global Before Each")
        every { clock.instant() } returns Instant.parse("2023-07-21T14:39:42Z")
        every { eventPublisher.publish(any()) } just runs
    }

    @Nested
    @DisplayName("Borrow Book")
    inner class BorrowBook {

        @BeforeEach
        fun setup() {
            println("Nested Before Each")
        }

        @Test
        fun `successfully borrows a book`() {
            //Act
            every { repository.update(bookId, any()) } answers {
                val update = secondArg<(Book) -> Book>()
                update(book)
            }
            val result = cut.borrowBook(bookId, borrower)

            //Assert
            assertTrue(result is Either.Right)
            result as Either.Right<Book>
            with(result.value) {
                id shouldBe bookId
                data shouldBe book.data
                state shouldBe State.Borrowed(
                    Borrower("Gandalf"),
                    Instant.parse("2023-07-21T14:39:42Z")
                )
            }
        }

        @Test
        fun `publishes event when book is borrowed`() {
            var updatedBook: Book? = null
            every { repository.update(bookId, any()) } answers {
                val update = secondArg<(Book) -> Book>()
                updatedBook = update(book)
                updatedBook
            }

            cut.borrowBook(bookId, borrower)

            verify { eventPublisher.publish(BookBorrowedEvent(updatedBook!!)) }
        }

        @Test
        fun `returns failure when Book does not exist`() {
            every { repository.update(bookId, any()) } returns null

            val result = cut.borrowBook(bookId, borrower)
            assertTrue(result is Either.Left)
            result as Either.Left<BookFailure>
            assertTrue(result.value is BookNotFound)

            verify { eventPublisher wasNot called }
        }

        @Test
        fun `does not publish event when Book does not exist`() {
            every { repository.update(bookId, any()) } returns null

            cut.borrowBook(bookId, borrower)

            verify { eventPublisher wasNot called }
        }
    }
}
