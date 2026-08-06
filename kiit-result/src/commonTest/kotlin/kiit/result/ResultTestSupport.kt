package kiit.result

import kiit.codes.Err
import kiit.codes.Status
import kotlin.test.assertEquals

interface ResultTestSupport {
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
                is Err.ErrorInfo -> assertEquals(expectedError, it.msg)
                else -> throw Exception("Unexpected for : $it")
            }
        }
    }
}
