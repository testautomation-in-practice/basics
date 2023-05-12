package example.spring.boot.domains.books.model.primitives

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class NumberOfPagesTest {

    @Nested
    inner class Validation {

        @ParameterizedTest
        @ValueSource(ints = [1, 1234, 10_000])
        fun `numbers inside the bounds valid`(n: Int) {
            assertDoesNotThrow { NumberOfPages(n) }
        }

        @ParameterizedTest
        @ValueSource(ints = [-99, 0, 10_001, 654321])
        fun `numbers outside the bounds invalid`(n: Int) {
            val ex = assertThrows<IllegalArgumentException> {
                NumberOfPages(n)
            }
            assertThat(ex).hasMessageContaining("[$n] is not a valid number of pages!")
        }
    }

    @Nested
    inner class Representation {

        @Test
        fun `string representation is delegated to the inner value`() {
            val value = NumberOfPages(42)

            assertThat(value.toString()).isEqualTo("42")
        }

        @Test
        fun `integer representation is the inner value`() {
            val value = NumberOfPages(42)

            assertThat(value.toInt()).isEqualTo(42)
        }

    }

}