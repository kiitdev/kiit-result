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

/**
 * These tests the building/construction of the Result model in simple/advance cases
 * 1. defaults ( no parameters )
 * 2. with message
 * 3. with explicit status
 */
class ResultBuilderTests : ResultTestSupport, OutcomeBuilder {
    @Test
    fun can_build_successes() {
        val status = Succeeded.SUCCESS
        ensureSuccess(success<Int>(), status, null)
        ensureSuccess(success(42), status, 42)
        ensureSuccess(success(42, "life"), status, 42, "life")
        ensureSuccess(success(42, status), status, 42)
    }

    @Test
    fun can_build_pending() {
        val status = Pending.ACCEPTED
        ensureSuccess(pending<Int>(), status, null)
        ensureSuccess(pending(42), status, 42)
        ensureSuccess(pending(42, "life"), status, 42, "life")
        ensureSuccess(pending(42, status), status, 42)
    }

    @Test
    fun can_build_excluded() {
        // Excluded is a Passed category — an excluded item is a success, not a failure.
        val status = Excluded.SKIPPED
        ensureSuccess(excluded<Int>(), status, null)
        ensureSuccess(excluded(42), status, 42)
        ensureSuccess(excluded(42, "skipped-x"), status, 42, "skipped-x")
        ensureSuccess(excluded(42, status), status, 42)
    }

    @Test
    fun can_build_invalid() {
        val status = Invalid.INVALID_VALUE
        ensureFailure(invalid<Int>(), status, expectedError = status.message)
        ensureFailure(invalid<Int>("invalid-x"), status, expectedError = "invalid-x")
        ensureFailure(invalid<Int>(Exception("invalid-x")), status, expectedError = "invalid-x")
        ensureFailure(invalid<Int>(Err.of("invalid-x")), status, expectedError = "invalid-x")
    }

    @Test
    fun can_build_restricted() {
        val status = Restricted.DENIED
        ensureFailure(restricted<Int>(), status, expectedError = status.message)
        ensureFailure(restricted<Int>("restricted-x"), status, expectedError = "restricted-x")
        ensureFailure(restricted<Int>(Exception("restricted-x")), status, expectedError = "restricted-x")
        ensureFailure(restricted<Int>(Err.of("restricted-x")), status, expectedError = "restricted-x")
    }

    @Test
    fun can_build_rejected() {
        val status = Rejected.RULE_VIOLATION
        ensureFailure(rejected<Int>(), status, expectedError = status.message)
        ensureFailure(rejected<Int>("rejected-x"), status, expectedError = "rejected-x")
        ensureFailure(rejected<Int>(Exception("rejected-x")), status, expectedError = "rejected-x")
        ensureFailure(rejected<Int>(Err.of("rejected-x")), status, expectedError = "rejected-x")
    }

    @Test
    fun can_build_unserved() {
        val status = Unserved.UNEXPECTED
        ensureFailure(Outcomes.unserved<Int>(), status, expectedError = status.message)
        ensureFailure(Outcomes.unserved<Int>("unserved-x"), status, expectedError = "unserved-x")
        ensureFailure(Outcomes.unserved<Int>(Exception("unserved-x")), status, expectedError = "unserved-x")
        ensureFailure(Outcomes.unserved<Int>(Err.of("unserved-x")), status, expectedError = "unserved-x")
    }
}
