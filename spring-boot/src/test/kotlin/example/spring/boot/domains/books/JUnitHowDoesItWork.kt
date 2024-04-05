package example.spring.boot.domains.books

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.Locale

class SwitchLocaleToFrance : BeforeAllCallback, AfterAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        val store = getStore(context)
        store.put("originalLocale", Locale.getDefault())
        Locale.setDefault(Locale.FRANCE)
    }

    override fun afterAll(context: ExtensionContext) {
        val store = getStore(context)
        Locale.setDefault(store.get("originalLocale", Locale::class.java))
    }

    private fun getStore(context: ExtensionContext): ExtensionContext.Store =
        context.getStore(ExtensionContext.Namespace.GLOBAL)
}

@TestInstance(PER_CLASS)
@ExtendWith(SwitchLocaleToFrance::class)
class JUnitHowDoesItWork {

    @BeforeAll
    fun beforeAllLevel0() {
        println("beforeAll " + Locale.getDefault())
    }

    @AfterAll
    fun afterAllLevel0() {
        println("AfterAll " + Locale.getDefault())
    }

    @BeforeEach
    fun beforeEachLevel0() {
        println("beforeEachLevel0 " + Locale.getDefault())
    }

    @AfterEach
    fun afterEachLevel0() {
        println("afterEachLevel0 " + Locale.getDefault())
    }

    @Test
    fun `my test #1`() {
        println("my-test-1 " + Locale.getDefault())
    }

    @Nested
    inner class Nested1 {

        @BeforeEach
        fun beforeEachLevel1() {
            println("beforeEachLevel1 " + Locale.getDefault())
        }

        @Test
        fun `my test #2`() {
            println("my-test-2 " + Locale.getDefault())
        }
    }
}
