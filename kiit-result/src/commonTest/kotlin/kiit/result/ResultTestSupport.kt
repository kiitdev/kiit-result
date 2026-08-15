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
     * [expectedStatus] (or [expectedMessage], if the message was overridden from the default).
     */
    fun <T> ensureSuccess(
        result: Result<T, Err>,
        expectedStatus: Status,
        expectedValue: T,
        expectedMessage: String? = null
    ) {
        assertEquals(true, result.success)
        assertEquals(expectedMessage ?: expectedStatus.message, result.status.message)
        assertEquals(expectedMessage ?: expectedStatus.message, result.message)

        result.onSuccess { value ->
            assertEquals(expectedValue, value)
        }
    }

    /**
     * Asserts [result] is a [Failure] with a status matching [expectedStatus] (or
     * [expectedStatusMsg], if the message was overridden), and an error value matching
     * [expectedError], either directly (`String`) or via [Err.ErrorInfo.message].
     */
    fun <T> ensureFailure(
        result: Result<T, *>,
        expectedStatus: Status,
        expectedStatusMsg: String? = null,
        expectedError: String? = null
    ) {
        assertEquals(false, result.success)
        assertEquals(expectedStatusMsg ?: expectedStatus.message, result.status.message)
        assertEquals(expectedStatusMsg ?: expectedStatus.message, result.message)
        result.onFailure {
            when (it) {
                is String -> assertEquals(expectedError, it)
                is Err.ErrorInfo -> assertEquals(expectedError, it.message)
                else -> throw Exception("Unexpected for : $it")
            }
        }
    }
}
