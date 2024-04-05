package example.spring.boot.domains.books.model.primitives

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class NumberOfPagesTests {

    @ParameterizedTest
    @ValueSource(ints = [1, 5_000, 10_000])
    fun `valid examples`(example: Int) {
        assertDoesNotThrow {
            NumberOfPages(example)
        }
    }

    @TestFactory
    fun `valid examples 2`(): List<DynamicTest> =
        listOf(1, (2..9_999).random(), 10_000)
            .map { example ->
                dynamicTest("$example") {
                    assertDoesNotThrow {
                        NumberOfPages(example)
                    }
                }
            }

    @ParameterizedTest
    @ValueSource(ints = [0, 10_001])
    fun `invalid examples`(example: Int) {
        val ex = assertThrows<IllegalArgumentException> {
            NumberOfPages(example)
        }
        assertThat(ex).hasMessage("[$example] is not a valid number of pages! (must be between 1 and 10000)")
    }

    @Test
    fun `toInt returns the same value as the input`() {
        val cut = NumberOfPages(42)
        val result = cut.toInt()
        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `toSting returns the int value as the a String`() {
        val cut = NumberOfPages(42)
        val result = cut.toString()
        assertThat(result).isEqualTo("42")
    }
}
