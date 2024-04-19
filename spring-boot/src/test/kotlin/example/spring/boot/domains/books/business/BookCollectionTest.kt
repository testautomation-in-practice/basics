package example.spring.boot.domains.books.business

import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.events.BookAddedEvent
import example.spring.boot.domains.books.model.primitives.Author
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.NumberOfPages
import example.spring.boot.domains.books.model.primitives.Title
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.util.IdGenerator
import java.time.Clock
import java.util.UUID.randomUUID

internal class BookCollectionTest {

    private val idGenerator = mockk<IdGenerator>()
    private val repository = mockk<BookRepository>()
    private val eventPublisher = mockk<BookEventPublisher>()
    private val clock = mockk<Clock>()
    private val cut = BookCollection(repository, eventPublisher, idGenerator, clock)

    @Nested
    @DisplayName("Add Book")
    inner class AddBook {

        private val bookData = BookData(
            Isbn("1234567890"), Title("Lord of the Rings"),
            NumberOfPages(123), setOf(Author("JRR Tolkien"))
        )

        @BeforeEach
        fun setup() {
            every { idGenerator.generateId() } returns randomUUID()
            every { repository.insert(any()) } just runs
            every { eventPublisher.publish(any()) } just runs
        }

        @Test
        fun `returns book data after creating it`() {
            val book = cut.addBook(bookData)

            book.data shouldBe bookData
        }

        @Test
        fun `publishes a Book Added Event after creating a book`() {
            val result = cut.addBook(bookData)

            verify {
                eventPublisher.publish(BookAddedEvent(result))
            }
        }

        @Test
        fun `inserts the book after creating it`() {
            val book = cut.addBook(bookData)

            verify {
                repository.insert(book)
            }
        }

        @Test
        fun `fails when event-publisher throws an exception`() {
            val error = RuntimeException("ERROR")
            every {
                eventPublisher.publish(any())
            } throws error

            val exception = assertThrows<RuntimeException> {
                cut.addBook(bookData)
            }
            exception shouldBeSameInstanceAs error
        }
        
        @Test
        fun `does not publish an event when book cannot be inserted`() {
            val error = RuntimeException("ERROR")
            every {
                repository.insert(any())
            } throws error

            val exception = assertThrows<RuntimeException> {
                cut.addBook(bookData)
            }

            verify {
                eventPublisher wasNot called
            }

            exception shouldBeSameInstanceAs error
        }
    }
}
