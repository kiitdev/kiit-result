package kiit.result

import kiit.codes.Err
import kiit.codes.Status
import kotlin.test.assertEquals

/**
 * Shared assertion helpers for asserting on an [Outcome] (`Result<T, Err>`) in tests.
 */
interface ResultTestSupport {
    /**
     * Asserts [result] is a [Success] holding [expectedValue], with a status matching
     * [expectedStatus].
     */
    fun <T> ensureSuccess(
        result: Result<T, Err>,
        expectedStatus: Status,
        expectedValue: T
    ) {
        assertEquals(true, result.success)
        assertEquals(expectedStatus, result.status)

        result.onSuccess { value ->
            assertEquals(expectedValue, value)
        }
    }

    /**
     * Asserts [result] is a [Failure] with a status matching [expectedStatus], and an error value matching
     * [expectedError], either directly (`String`) or via [Err.ErrorInfo.message].
     */
    fun <T> ensureFailure(
        result: Result<T, *>,
        expectedStatus: Status,
        expectedError: String? = null
    ) {
        assertEquals(false, result.success)
        assertEquals(expectedStatus, result.status)
        result.onFailure {
            when (it) {
                is String -> assertEquals(expectedError, it)
                is Err.ErrorInfo -> assertEquals(expectedError, it.message)
                else -> throw Exception("Unexpected for : $it")
            }
        }
    }
}
