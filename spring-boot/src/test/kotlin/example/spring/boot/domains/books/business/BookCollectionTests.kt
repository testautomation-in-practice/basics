package example.spring.boot.domains.books.business

import example.spring.boot.domains.books.model.events.BookDeletedEvent
import io.github.logrecorder.api.LogRecord
import io.github.logrecorder.assertion.LogRecordAssertion.Companion.assertThat
import io.github.logrecorder.assertion.containsExactly
import io.github.logrecorder.junit5.RecordLoggers
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.util.IdGenerator
import org.springframework.util.JdkIdGenerator
import java.time.Clock
import java.util.UUID

class BookCollectionTests {

    private val repository: BookRepository = mockk()
    private val eventPublisher: BookEventPublisher = mockk(relaxed = true)
    private val idGenerator: IdGenerator = JdkIdGenerator()
    private val clock: Clock = Clock.systemUTC()

    private val cut = BookCollection(repository, eventPublisher, idGenerator, clock)

    private val bookId = UUID.randomUUID()

    @Nested
    inner class DeleteBook {

        @Test
        fun `when a book is actually deleted an event is published`() {
            every { repository.delete(bookId) } returns true
            cut.deleteBook(bookId)
            verify { eventPublisher.publish(BookDeletedEvent(bookId)) }
        }

        @Test
        @RecordLoggers(BookCollection::class)
        fun `when a book is actually deleted a debug message is logged`(log: LogRecord) {
            every { repository.delete(any()) } returns true

            cut.deleteBook(bookId)

            assertThat(log).containsExactly {
                info("deleting book with ID [$bookId]")
                debug("book successfully deleted")
            }
        }

        @Test
        fun `when a book is not actually deleted no events are published`() {
            every { repository.delete(any()) } returns false
            cut.deleteBook(bookId)
            verify { eventPublisher wasNot called }
        }

        @Test
        @RecordLoggers(BookCollection::class)
        fun `when a book is not actually deleted a warning message is logged`(log: LogRecord) {
            every { repository.delete(any()) } returns false

            cut.deleteBook(bookId)

            assertThat(log).containsExactly {
                info("deleting book with ID [$bookId]")
                warn("there is no book with ID [$bookId] - did not deleted anything")
            }
        }
    }
}
// wozu das returns true? -> delete() hat einen return wert. Der Mock weiß nicht was zu tun ist. Daher definieren wir das.

