package kiit.result

import kiit.codes.Succeeded
import kiit.result.Outcomes.success
import kiit.result.Outcomes.unserved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [List] of [Result] operators: `combine`, `partition`, `allSuccess`/`allFailure`, and
 * `anySuccess`/`anyFailure`.
 */
class ResultListOpsTests {
    @Test
    fun can_combine_all_success() {
        val first = success("a").withAction(Action("first"))
        val list = listOf(first, success("b"), success("c"))

        val combined = list.combine()
        assertEquals(listOf("a", "b", "c"), combined.getOrNull())
        assertEquals(Succeeded.SUCCESS, combined.status)
        assertEquals("first", combined.action?.action)
    }

    @Test
    fun can_combine_short_circuits_on_first_failure() {
        val failure = unserved<String>("boom")
        val list = listOf(success("a"), failure, success("c"))

        val combined = list.combine()
        assertEquals(false, combined.success)
        assertEquals("boom", combined.getErrorOrNull()?.message)
        assertEquals(failure.status, combined.status)
    }

    @Test
    fun can_partition() {
        val list = listOf(success("a"), unserved<String>("boom"), success("c"))

        val (values, errors) = list.partition()
        assertEquals(listOf("a", "c"), values)
        assertEquals(listOf("boom"), errors.map { it.message })
    }

    @Test
    fun can_check_all_success() {
        assertTrue(listOf(success("a"), success("b")).allSuccess())
        assertFalse(listOf(success("a"), unserved<String>("boom")).allSuccess())
    }

    @Test
    fun can_check_all_failure() {
        assertTrue(listOf(unserved<String>("boom"), unserved<String>("bang")).allFailure())
        assertFalse(listOf(success("a"), unserved<String>("boom")).allFailure())
    }

    @Test
    fun can_check_any_success() {
        assertTrue(listOf(unserved<String>("boom"), success("a")).anySuccess())
        assertFalse(listOf(unserved<String>("boom"), unserved<String>("bang")).anySuccess())
    }

    @Test
    fun can_check_any_failure() {
        assertTrue(listOf(success("a"), unserved<String>("boom")).anyFailure())
        assertFalse(listOf(success("a"), success("b")).anyFailure())
    }
}
