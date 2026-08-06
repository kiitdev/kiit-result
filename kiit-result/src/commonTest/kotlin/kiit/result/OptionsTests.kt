package kiit.result

import kiit.codes.Rejected
import kiit.codes.Succeeded
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the Option<T> = Result<T, Unit> alias and its Some/None-style entry points.
 */
class OptionsTests {
    @Test
    fun can_build_some() {
        val option = Options.some(42)
        assertEquals(true, option.success)
        assertEquals(Succeeded.SUCCESS, option.status)
        assertEquals(42, option.getOrNull())
    }

    @Test
    fun can_build_none() {
        assertEquals(false, Options.none<Int>().success)
        assertEquals(Rejected.NOT_EXISTS.message, Options.none<Int>().status.message)
        assertEquals("missing", Options.none<Int>("missing").status.message)
        assertEquals(Rejected.CONFLICT, Options.none<Int>(Rejected.CONFLICT).status)
    }
}
