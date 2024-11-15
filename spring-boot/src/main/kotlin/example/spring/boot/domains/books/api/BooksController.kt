package example.spring.boot.domains.books.api

import arrow.core.getOrElse
import example.spring.boot.common.security.Authorities.ROLE_CURATOR
import example.spring.boot.domains.books.business.BookCollection
import example.spring.boot.domains.books.business.BookCoverService
import example.spring.boot.domains.books.business.BookNotFound
import example.spring.boot.domains.books.business.BookUpdateFailed
import example.spring.boot.domains.books.model.BookData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.notFound
import org.springframework.http.ResponseEntity.ok
import org.springframework.http.ResponseEntity.status
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.time.Instant
import java.util.*

/**
 * Expose [BookCollection] functions as REST resources.
 */
@RestController
@RequestMapping("/api/books")
class BooksController(
    private val collection: BookCollection,
    private val bookCoverService: BookCoverService,
    private val clock: Clock,
) {

    /**
     * Create a new _Book_ based on the provided data.
     *
     * Returns the new _Book's_ representation model with a 201 Created status.
     */
    @PostMapping
    @ResponseStatus(CREATED)
    fun addBook(@RequestBody request: AddBookRequest): BookRepresentation {
        val bookData = BookData(isbn = request.isbn, title = request.title)
        val book = collection.addBook(bookData)
        return book.toRepresentation(userIsCurator())
    }

    /**
     * Tries to get an existing _Book_ by its ID.
     *
     * Returns the _Book's_ representation model with a 200 OK status if it was found.
     * Returns an empty response with 404 Not Found status if it was not found.
     */
    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: UUID): ResponseEntity<BookRepresentation> {
        val book = collection.getBook(id = id) ?: return notFound().build()
        val representation = book.toRepresentation(userIsCurator())
        return ok(representation)
    }

    /**
     * Tries to borrow existing _Book_ by its ID.
     *
     * Returns the borrowed _Book's_ representation model with a 200 OK status if it was found and borrowed.
     * Returns an empty response with 404 Not Found status if it was not found.
     * Returns an empty response with 409 Conflict status if it was found, but not in a borrowable state.
     */
    @PostMapping("/{id}/borrow")
    fun borrowBookById(
        @PathVariable id: UUID,
        @RequestBody request: BorrowBookRequest
    ): ResponseEntity<BookRepresentation> =
        collection.borrowBook(id = id, borrower = request.borrower)
            .map { it.toRepresentation(userIsCurator()) }
            .map { ok(it) }
            .getOrElse { failure ->
                when (failure) {
                    BookNotFound -> notFound().build()
                    BookUpdateFailed -> status(CONFLICT).build()
                }
            }

    /**
     * Tries to return existing _Book_ by its ID.
     *
     * Returns the returned _Book's_ representation model with a 200 OK status if it was found and returned.
     * Returns an empty response with 404 Not Found status if it was not found.
     * Returns an empty response with 409 Conflict status if it was found, but not in a returnable state.
     */
    @PostMapping("/{id}/return")
    fun returnBookById(@PathVariable id: UUID): ResponseEntity<*> =
        collection.returnBook(id = id)
            .map { it.toRepresentation(userIsCurator()) }
            .map { ok(it) }
            .getOrElse { failure ->
                when (failure) {
                    BookNotFound -> status(NOT_FOUND)
                        .body(
                            ErrorResponse(
                                message = "Book with id [$id] does not exist",
                                timestamp = Instant.now(clock)
                            )
                        )

                    BookUpdateFailed -> status(CONFLICT)
                        .body(
                            ErrorResponse(
                                message = "Book with id [$id] is not borrowed",
                                timestamp = Instant.now(clock)
                            )
                        )
                }
            }

    /**
     * Tries to delete a _Book_ by its ID.
     *
     * Returns an empty response with 204 No Content status in all cases.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    fun deleteBookById(@PathVariable id: UUID) {
        collection.deleteBook(id = id)
    }

    @GetMapping("/{id}/cover")
    fun bookCoverById(@PathVariable id: UUID): ResponseEntity<Resource> {
        val (byteStream, contentLength, contentType) = bookCoverService
            .runCatching { findCover(id) }
            .getOrElse { return notFound().build() }

        return ok()
            .contentLength(contentLength)
            .contentType(MediaType.parseMediaType(contentType))
            .body(InputStreamResource(byteStream))
    }

    @ResponseStatus(CREATED)
    @PutMapping("/{id}/cover")
    fun putCover(
        @PathVariable("id") id: UUID,
        @RequestParam("cover") cover: MultipartFile,
    ) {
        bookCoverService.saveCover(id, cover.bytes, contentType = cover.contentType!!)
    }

    fun userIsCurator(): Boolean =
        SecurityContextHolder.getContext().authentication.authorities.any { it.authority == ROLE_CURATOR }

}
