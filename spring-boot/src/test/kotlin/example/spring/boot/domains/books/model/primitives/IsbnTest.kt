package example.spring.boot.domains.books.model.primitives

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IsbnTest {

    @Nested
    inner class Validation {

        @ParameterizedTest
        @ValueSource(
            strings = [
                "123456789",       // 9 digits
                "123-45678901",    // 11 digits
                "123-456789012",   // 12 digits
                "123-45678901234", // 14 digits
            ]
        )
        fun `values that are not 10 or 13 digits long are invalid`(value: String) {
            val ex = assertThrows<IllegalArgumentException> {
                Isbn(value)
            }
            assertThat(ex).hasMessageContaining("[$value] is not a valid 10 or 13 digit ISBN!")
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "1234567890",
                "123-1234567890",
            ]
        )
        fun `values that are 10 or 13 digits long are valid`(value: String) {
            assertDoesNotThrow { Isbn(value) }
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "  ",
                "qwertzuiop",
                "a234567890",
                "123-123456789$",
            ]
        )
        fun `values must be made up of only digits`(value: String) {
            val ex = assertThrows<IllegalArgumentException> {
                Isbn(value)
            }
            assertThat(ex).hasMessageContaining("[$value] is not a valid 10 or 13 digit ISBN!")
        }

        @Test
        fun `Isbn with 13 digits must have separator after the first 3 digits`() {
            val value = "1231234567890"
            val ex = assertThrows<IllegalArgumentException> {
                Isbn(value)
            }
            assertThat(ex).hasMessageContaining("[$value] is not a valid 10 or 13 digit ISBN!")
        }

    }

    @Nested
    inner class Representation {

        @Test
        fun `string representation is delegated to the inner value`() {
            val value = Isbn("1234567890")

            assertThat(value.toString()).isEqualTo("1234567890")
        }
    }
}