package example.spring.boot.domains.books.persistence

import example.spring.boot.MongoDBInitializer
import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.State
import example.spring.boot.domains.books.model.primitives.Author
import example.spring.boot.domains.books.model.primitives.Borrower
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.NumberOfPages
import example.spring.boot.domains.books.model.primitives.Title
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

const val NOW = "2022-01-08T12:00:00Z"

@DataMongoTest
@Import(MongoDbBookRepository::class, TestConfig::class)
@ContextConfiguration(initializers = [MongoDBInitializer::class])
class MongoDbBookRepositoryTest @Autowired constructor(
    private val cut: MongoDbBookRepository,
    private val bookRepo: BookDocumentRepository,
    private val clock: Clock,
) {

    private val bookId = UUID.randomUUID()
    private val bookData = BookData(
        isbn = Isbn("1234567890"),
        title = Title("Lord of the Rings"),
        numberOfPages = NumberOfPages(1234),
        authors = setOf(Author("J.R.R. Tolkien")),
    )

    @Test
    fun `correctly insert new Book`() {
        val book = Book(
            id = bookId,
            data = bookData,
            state = State.Borrowed(
                by = Borrower("Gandalf"),
                at = Instant.parse(NOW)
            ),
        )

        cut.insert(book)

        bookRepo.findAll() shouldContainExactlyInAnyOrder listOf(
            BookDocument(
                id = bookId,
                data = bookData,
                borrowed = State.Borrowed(
                    by = Borrower("Gandalf"),
                    at = Instant.parse(NOW)
                ),
                created = clock.instant(),
                lastUpdated = clock.instant(),
            )
        )
    }

}

private class TestConfig {

    @Bean
    fun clock(): Clock = Clock.fixed(Instant.parse(NOW), ZoneId.of("UTC"))
    // ISO-8601
}
