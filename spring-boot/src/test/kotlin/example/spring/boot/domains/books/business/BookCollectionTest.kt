package example.spring.boot.domains.books.business

import example.spring.boot.domains.books.model.*
import example.spring.boot.domains.books.model.events.BookAddedEvent
import example.spring.boot.domains.books.model.events.BookDeletedEvent
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.util.IdGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

class BookCollectionTest {

    val repository: BookRepository = mockk()
    val eventPublisher: BookEventPublisher = mockk()
    val idGenerator: IdGenerator = mockk()
    val clock: Clock = Clock.fixed(Instant.parse("2023-05-05T12:34:56.789Z"), ZoneId.of("UTC"))
    val cut = BookCollection(repository, eventPublisher, idGenerator, clock)

    @Nested
    @DisplayName("Add Book")
    inner class AddBook {

        @BeforeEach
        fun setup() {
            every { idGenerator.generateId() } returns Ids.LORD_OF_THE_RINGS
            every { repository.insert(any()) } just runs
        }

        @Test
        fun `inserting a book publishes an event`() {
            val eventSlot = slot<BookAddedEvent>()
            every { eventPublisher.publish(capture(eventSlot)) } just runs

            cut.addBook(BooksData.LORD_OF_THE_RINGS)

            eventSlot.captured.book.also { book ->
                assertThat(book.id).isEqualTo(Ids.LORD_OF_THE_RINGS)
                assertThat(book.state).isEqualTo(State.Available)
                assertThat(book.data).isEqualTo(BooksData.LORD_OF_THE_RINGS)
            }
        }

        @Test
        fun `inserting a book returns the inserted book`() {
            every { eventPublisher.publish(any()) } just runs

            val addedBook = cut.addBook(BooksData.LORD_OF_THE_RINGS)

            assertThat(addedBook.id).isEqualTo(Ids.LORD_OF_THE_RINGS)
            assertThat(addedBook.state).isEqualTo(State.Available)
            assertThat(addedBook.data).isEqualTo(BooksData.LORD_OF_THE_RINGS)
        }
    }

    @Nested
    @DisplayName("Get Book")
    inner class GetBook {

        @Test
        fun `returns null when book can not be found`() {
            every { repository.get(Ids.LORD_OF_THE_RINGS) } returns null

            val result = cut.getBook(Ids.LORD_OF_THE_RINGS)

            assertThat(result).isNull()
        }

        @Test
        fun `returns book when it can be found`() {
            val book = Book(
                id = Ids.LORD_OF_THE_RINGS,
                state = State.Available,
                data = BooksData.LORD_OF_THE_RINGS
            )
            every { repository.get(Ids.LORD_OF_THE_RINGS) } returns book

            val result = cut.getBook(Ids.LORD_OF_THE_RINGS)

            assertThat(result).isEqualTo(book)
        }
    }

    @Nested
    @DisplayName("Delete Book")
    inner class DeleteBook {

        @BeforeEach
        fun setup() {
            every { eventPublisher.publish(any()) } just runs
        }

        @Test
        fun `publishes an event when book existed and was deleted`() {
            every { repository.delete(Ids.LORD_OF_THE_RINGS) } returns true

            cut.deleteBook(Ids.LORD_OF_THE_RINGS)

            verify { eventPublisher.publish(BookDeletedEvent(Ids.LORD_OF_THE_RINGS)) }
        }

        @Test
        fun `does not publish an event when book was deleted already`() {
            every { repository.delete(Ids.LORD_OF_THE_RINGS) } returns false

            cut.deleteBook(Ids.LORD_OF_THE_RINGS)

            verify(exactly = 0) { eventPublisher.publish(any()) }
        }
    }

}