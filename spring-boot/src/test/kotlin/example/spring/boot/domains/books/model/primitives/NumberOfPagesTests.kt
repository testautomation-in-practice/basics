package example.spring.boot.domains.books.model.primitives

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.throwable.shouldHaveMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class NumberOfPagesTests {

    @ParameterizedTest
    @ValueSource(ints = [1, 5_000, 10_000])
    fun `valid number of pages don't throw any exceptions`(example: Int) {
        shouldNotThrowAny {
            NumberOfPages(example)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 10_001])
    fun `invalid number of pages throw an exceptions`(example: Int) {
        val ex = shouldThrow<IllegalArgumentException> {
            NumberOfPages(example)
        }
        ex shouldHaveMessage "[$example] is not a valid number of pages! (must be between 1 and 10000)"
    }

    @Test
    fun `value passed to constructor can be retrieved by toInt`() {
        val cut = NumberOfPages(42)
        val result = cut.toInt()
        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `value passed to constructor can be retrieved as a string`() {
        val cut = NumberOfPages(1337)
        val result = cut.toString()
        assertThat(result).isEqualTo("1337")
    }
}
