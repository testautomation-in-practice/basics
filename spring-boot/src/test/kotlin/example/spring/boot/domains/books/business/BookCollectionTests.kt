package example.spring.boot.domains.books.business

import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.State.Available
import example.spring.boot.domains.books.model.events.BookAddedEvent
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import io.github.logrecorder.api.LogRecord
import io.github.logrecorder.assertion.shouldContainExactly
import io.github.logrecorder.junit5.RecordLoggers
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.util.IdGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class BookCollectionTests {

    private val repository: BookRepository = mockk(relaxUnitFun = true)
    private val eventPublisher: BookEventPublisher = mockk(relaxUnitFun = true)
    private val idGenerator: IdGenerator = mockk()
    private val clock: Clock = Clock.fixed(Instant.parse("2024-11-08T12:34:56.789Z"), ZoneId.of("UTC"))
    private val cut = BookCollection(repository, eventPublisher, idGenerator, clock)

    private val bookId = UUID.randomUUID()
    private val lordOfTheRings = BookData(
        isbn = Isbn("978-0008471286"),
        title = Title("The Lord of the Rings: The Classic Bestselling Fantasy Novel")
    )
    private val lordOfTheRingsBook = Book(id = bookId, data = lordOfTheRings, state = Available)

    @BeforeEach
    fun stubDefaultBehaviour() {
        every { idGenerator.generateId() } returns bookId
    }

    @Nested
    inner class AddBook {

        @Test
        fun `adding a book returns the newly created Book`() {
            val result = cut.addBook(lordOfTheRings)
            result shouldBe lordOfTheRingsBook
        }

        @Test
        fun `adding a book stores it in the database`() {
            cut.addBook(lordOfTheRings)
            verify { repository.insert(lordOfTheRingsBook) }
        }

        @Test
        fun `adding a book publishes an event`() {
            cut.addBook(lordOfTheRings)
            verify { eventPublisher.publish(BookAddedEvent(lordOfTheRingsBook)) }
        }

        @Test
        @RecordLoggers(BookCollection::class)
        fun `adding a book produces logs`(log: LogRecord) {
            cut.addBook(lordOfTheRings)
            log shouldContainExactly {
                info(startsWith("creating new book ["), endsWith("]"))
                debug("book with ID [$bookId] was created")
            }
        }

        @Test
        fun `repository exception are NOT handled`() {
            val exception = RuntimeException("oops")
            every { repository.insert(any()) } throws exception
            val ex = shouldThrowAny {
                cut.addBook(lordOfTheRings)
            }
            ex shouldBe exception
        }
    }
}

