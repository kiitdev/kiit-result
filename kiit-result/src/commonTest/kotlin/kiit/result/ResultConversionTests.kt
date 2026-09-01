package kiit.result

import kiit.codes.Restricted
import kiit.codes.Unserved
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests [Result.toOutcome]/[Result.toTry]'s status/error content consistency, especially the
 * null-error edge case where there's no error value to derive an Err/Exception message from.
 */
class ResultConversionTests {
    @Test
    fun to_outcome_with_null_error_retains_status_in_both_outer_status_and_err_message() {
        val failure: Result<Int, String?> = Failure<String?>(null, Restricted.DENIED)

        val retained = failure.toOutcome(retainStatus = true)
        assertEquals(Restricted.DENIED, retained.status)
        assertEquals(Restricted.DENIED.message, retained.getErrorOrNull()?.message)
    }

    @Test
    fun to_outcome_with_null_error_and_discarded_status_stays_consistent() {
        val failure: Result<Int, String?> = Failure<String?>(null, Restricted.DENIED)

        val discarded = failure.toOutcome(retainStatus = false)
        assertEquals(Unserved.UNEXPECTED, discarded.status)
        assertEquals(Unserved.UNEXPECTED.message, discarded.getErrorOrNull()?.message)
    }

    @Test
    fun to_outcome_with_non_null_error_is_unaffected() {
        val failure: Result<Int, String?> = Failure("boom", Restricted.DENIED)

        val outcome = failure.toOutcome()
        assertEquals(Restricted.DENIED, outcome.status)
        assertEquals("boom", outcome.getErrorOrNull()?.message)
    }
}
