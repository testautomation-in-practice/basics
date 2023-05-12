package example.spring.boot.domains.books.model

import example.spring.boot.domains.books.model.primitives.*
import java.time.Instant
import java.util.*


object Ids {
    val LORD_OF_THE_RINGS: UUID = UUID.randomUUID()
}

object BooksData {
    val LORD_OF_THE_RINGS = BookData(
        Isbn("1234567890"),
        Title("Lord of the Rings"),
        NumberOfPages(500),
        setOf(Author("J.R.R. Tolkien"))
    )

    val LORD_OF_THE_RINGS_UNENRICHED = BookData(
        Isbn("1234567890"),
        Title("Lord of the Rings")
    )
}

object Books {
    val LORD_OF_THE_RINGS = Book(
        id = Ids.LORD_OF_THE_RINGS,
        data = BooksData.LORD_OF_THE_RINGS,
        state = State.Available
    )
    val LORD_OF_THE_RINGS_BORROWED = Book(
        id = Ids.LORD_OF_THE_RINGS,
        data = BooksData.LORD_OF_THE_RINGS,
        state = State.Borrowed(by = Borrower("Frodo"), at = Instant.parse("2023-05-05T12:34:56.789Z"))
    )
    val LORD_OF_THE_RINGS_UNENRICHED = Book(
        id = Ids.LORD_OF_THE_RINGS,
        data = BooksData.LORD_OF_THE_RINGS_UNENRICHED,
        state = State.Available
    )
}
