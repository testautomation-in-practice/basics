package example.spring.boot.domains.books.persistence

import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.State
import example.spring.boot.domains.books.model.primitives.Borrower
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID.randomUUID

@DataMongoTest
@Import(MongoDbBookRepository::class, ClockConfig::class)
@InitializeWithContainerizedMongoDB
class MongoDbBookRepositoryTest @Autowired constructor(
    private val cut: MongoDbBookRepository,
    private val repo: BookDocumentRepository,
    private val clock: Clock,
) {

    private val id = randomUUID()

    @BeforeEach
    fun setup() {
        repo.insert(
            BookDocument(
                id = id,
                borrowed = null,
                created = Instant.now(),
                lastUpdated = Instant.now(),
                data = BookData(
                    isbn = Isbn("1234567890"),
                    title = Title("The Lord of the Rings"),
                ),
            )
        )
    }

    @AfterEach
    fun cleanup() {
        repo.deleteAll()
    }

    @Test
    fun `does nothing when book does not exist`() {
        val result = cut.update(randomUUID()) {
            throw IllegalStateException("should never happen")
        }

        result shouldBe null
    }

    @Test
    fun `updates book when it exists`() {
        val borrowedByGandalf = State.Borrowed(Borrower("Gandalf"), at = Instant.now())

        val result = cut.update(id) { book ->
            book.copy(state = borrowedByGandalf)
        }!!

        result.state shouldBe borrowedByGandalf
    }

    @Test
    fun `updates lastUpdated time stamp`() {
        cut.update(id) { it }!!

        val updatedDocument = repo.findById(id).get()

        updatedDocument.lastUpdated shouldBe clock.instant()
    }
}

private class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.fixed(
        Instant.parse("2020-01-08T12:00:00Z"),
        ZoneId.systemDefault(),
    )
}