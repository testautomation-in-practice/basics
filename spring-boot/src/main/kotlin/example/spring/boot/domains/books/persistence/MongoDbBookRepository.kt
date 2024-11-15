package example.spring.boot.domains.books.persistence

import example.spring.boot.domains.books.business.BookRepository
import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.State.Available
import example.spring.boot.domains.books.model.State.Borrowed
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.Clock
import java.util.*

@Component
class MongoDbBookRepository(
    private val repository: BookDocumentRepository,
    private val clock: Clock
) : BookRepository {

    override fun insert(book: Book) {
        val now = clock.instant()
        val document = BookDocument(
            id = book.id,
            data = book.data,
            borrowed = book.state as? Borrowed,
            created = now,
            lastUpdated = now
        )
        repository.insert(document)
    }

    override fun get(id: UUID): Book? {
        val document = repository.findById(id).orElse(null)
        return document?.toBook()
    }

    override fun update(id: UUID, block: (Book) -> Book): Book? {
        val document = repository.findById(id).orElse(null) ?: return null
        val updatedBook = block(document.toBook())

        val updatedDocument = document.copy(
            data = updatedBook.data,
            borrowed = updatedBook.state as? Borrowed,
            lastUpdated = clock.instant()
        )
        val savedDocument = repository.save(updatedDocument)

        return savedDocument.toBook()
    }

    override fun delete(id: UUID): Boolean {
        if (repository.existsById(id)) {
            repository.deleteById(id)
            return true
        }
        return false
    }

    private fun BookDocument.toBook(): Book =
        Book(id = id, data = data, state = borrowed ?: Available)
}

@Repository
interface BookDocumentRepository : MongoRepository<BookDocument, UUID>
