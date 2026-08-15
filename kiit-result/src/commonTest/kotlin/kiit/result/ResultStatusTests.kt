package kiit.result

import kiit.codes.Err
import kiit.codes.Excluded
import kiit.codes.Invalid
import kiit.codes.Pending
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.Succeeded
import kiit.codes.Unserved
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the Status taxonomy as seen through Result construction:
 * 1. the success flag per group
 * 2. building Success/Failure with defaults, messages, and explicit statuses
 */
class ResultStatusTests : ResultTestSupport {
    @Test
    fun can_test_success() {
        assertEquals(true, Succeeded.SUCCESS.success)
        assertEquals(true, Pending.ACCEPTED.success)
        // Excluded is a Passed group, an excluded item is still a success.
        assertEquals(true, Excluded.SKIPPED.success)
        assertEquals(false, Invalid.BAD_REQUEST.success)
        assertEquals(false, Restricted.UNAUTHENTICATED.success)
        assertEquals(false, Rejected.CONFLICT.success)
        assertEquals(false, Unserved.UNEXPECTED.success)
    }

    @Test
    fun can_create_success() {
        val status = Succeeded.SUCCESS
        ensureSuccess(Success(42), status, 42)
        ensureSuccess(Success(42, "created"), status, 42, "created")
        ensureSuccess(Outcomes.pending(42), Pending.ACCEPTED, 42)
    }

    @Test
    fun can_create_failure() {
        val status = Unserved.UNEXPECTED
        ensureFailure<Int>(Failure("invalid email"), status, expectedError = "invalid email")
        ensureFailure<Int>(
            Failure("invalid email", "bad data"),
            status,
            expectedStatusMsg = "bad data",
            expectedError = "invalid email",
        )
    }

    @Test
    fun can_create_failure_as_err() {
        val status = Unserved.UNEXPECTED
        ensureFailure<Int>(Failure(Err.of("invalid email")), status, expectedError = "invalid email")
        ensureFailure<Int>(
            Failure(Err.of("invalid email"), "bad data"),
            status,
            expectedStatusMsg = "bad data",
            expectedError = "invalid email",
        )
        ensureFailure<Int>(
            Outcomes.invalid(Err.of("invalid email"), Invalid.BAD_REQUEST),
            Invalid.BAD_REQUEST,
            expectedError = "invalid email",
        )
    }
}
