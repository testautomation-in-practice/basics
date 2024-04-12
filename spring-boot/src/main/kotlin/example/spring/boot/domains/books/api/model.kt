package example.spring.boot.domains.books.api

import example.spring.boot.domains.books.model.Book
import example.spring.boot.domains.books.model.BookData
import example.spring.boot.domains.books.model.State
import example.spring.boot.domains.books.model.primitives.Borrower
import example.spring.boot.domains.books.model.primitives.Isbn
import example.spring.boot.domains.books.model.primitives.Title
import java.util.*

/**
 * Data structure used as external representation of the internal model.
 *
 * @property id see [Book.id]
 * @property data see [Book.data]
 * @property available whether this book is currently available based on its internal status
 * @property borrowed if this book is currently borrowed to someone, this will contain data about that.
 * Will only be visible to _curator_ users.
 */
data class BookRepresentation(
    val id: UUID,
    val data: BookData,
    val available: Boolean,
    val borrowed: State.Borrowed?
)

fun Book.toRepresentation(includeBorrowed: Boolean) =
    BookRepresentation(
        id = id,
        data = data,
        available = state is State.Available,
        borrowed = if (includeBorrowed) state as? State.Borrowed else null
    )

data class AddBookRequest(
    val isbn: Isbn,
    val title: Title
)

data class BorrowBookRequest(
    val borrower: Borrower
)
