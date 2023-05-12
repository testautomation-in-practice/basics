package example.spring.boot.domains.books.persistence

import example.spring.boot.domains.books.model.Books
import example.spring.boot.domains.books.model.BooksData
import example.spring.boot.domains.books.model.Ids
import example.spring.boot.domains.books.model.State
import example.spring.boot.domains.books.model.primitives.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.UUID.randomUUID

@DataMongoTest
@ActiveProfiles("test")
@Import(MongoDbBookRepository::class, AdditionalBeans::class)
internal class MongoDbBookRepositoryTest(
    @Autowired val cut: MongoDbBookRepository,
    @Autowired val bookDocumentRepository: BookDocumentRepository,
    @Autowired val clock: Clock,
) {

    @AfterEach
    fun cleanup() {
        bookDocumentRepository.deleteAll()
    }

    @Nested
    inner class Insert {

        @Test
        fun `inserts book with timestamps for the current time`() {
            cut.insert(Books.LORD_OF_THE_RINGS)

            val document = bookDocumentRepository.findById(Ids.LORD_OF_THE_RINGS)
            assertThat(document).isPresent
            document.get().also { doc ->
                assertThat(doc.id).isEqualTo(Ids.LORD_OF_THE_RINGS)
                assertThat(doc.data).isEqualTo(BooksData.LORD_OF_THE_RINGS)
                assertThat(doc.borrowed).isNull()
                assertThat(doc.created).isEqualTo(clock.instant())
                assertThat(doc.lastUpdated).isEqualTo(clock.instant())
            }
        }
    }

    @Nested
    inner class Get {

        @Test
        fun `returns null when book does not exist`() {
            val id = randomUUID()
            val result = cut.get(id)

            assertThat(result).isNull()
        }

        @Test
        fun `returns book if it does exist`() {
            cut.insert(Books.LORD_OF_THE_RINGS)

            val result = cut.get(Ids.LORD_OF_THE_RINGS)!!

            assertThat(result.id).isEqualTo(Ids.LORD_OF_THE_RINGS)
            assertThat(result.data).isEqualTo(BooksData.LORD_OF_THE_RINGS)
            assertThat(result.state).isEqualTo(State.Available)
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `returns null when book does not exist`() {
            val result = cut.update(Ids.LORD_OF_THE_RINGS) { book ->
                book.copy(data = book.data.copy(numberOfPages = NumberOfPages(123)))
            }

            assertThat(result).isNull()
        }

        @Test
        fun `returns updated book when it does exist`() {
            cut.insert(Books.LORD_OF_THE_RINGS)
            val newNumberOfPages = NumberOfPages(123)
            val result = cut.update(Ids.LORD_OF_THE_RINGS) { book ->
                book.copy(data = book.data.copy(numberOfPages = newNumberOfPages))
            }!!

            assertThat(result.data.numberOfPages).isEqualTo(newNumberOfPages)
        }

    }

    @Nested
    inner class Delete {

        @Test
        fun `returns false if book does not exist`() {
            val result = cut.delete(Ids.LORD_OF_THE_RINGS)

            assertThat(result).isEqualTo(false)
        }

        @Test
        fun `returns true if book does exist`() {
            cut.insert(Books.LORD_OF_THE_RINGS)
            val result = cut.delete(Ids.LORD_OF_THE_RINGS)

            assertThat(result).isEqualTo(true)
        }
    }
}

private class AdditionalBeans {
    @Bean
    fun clock(): Clock = Clock.fixed(Instant.parse("2023-05-05T12:34:56.789Z"), ZoneId.of("UTC"))
}