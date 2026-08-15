package kiit.result

import kiit.codes.Err
import kiit.codes.Invalid
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the Validated<T> = Result<T, Err.ErrorList> alias and its [Validations] builder.
 */
class ValidationsTests {
    @Test
    fun can_build_success_when_no_errors() {
        val result = Validations.of("alice", emptyList())
        assertEquals(true, result.success)
        assertEquals("alice", result.getOrNull())
    }

    @Test
    fun can_build_failure_with_default_message_and_status() {
        val errors = listOf(Err.on("name", "", "Name is required"))
        val result = Validations.of("alice", errors)

        assertEquals(false, result.success)
        assertEquals(Invalid.INVALID_VALUE, result.status)
        result.onFailure { errorList ->
            assertEquals(errors, errorList.errors)
            assertEquals("Validation failed with 1 error(s)", errorList.message)
        }
    }

    @Test
    fun can_build_failure_collecting_multiple_errors() {
        val errors =
            listOf(
                Err.on("name", "", "Name is required"),
                Err.on("email", "bad", "Email must contain @"),
                Err.on("phone", "123", "Phone must be 10 digits"),
            )
        val result = Validations.of("alice", errors)

        result.onFailure { errorList ->
            assertEquals(3, errorList.errors.size)
            assertEquals(errors, errorList.errors)
            assertEquals("Validation failed with 3 error(s)", errorList.message)
        }
    }

    @Test
    fun can_override_message_and_status() {
        val errors = listOf(Err.on("name", "", "Name is required"))
        val result = Validations.of("alice", errors, message = "custom message", status = Invalid.BAD_REQUEST)

        assertEquals(Invalid.BAD_REQUEST, result.status)
        result.onFailure { errorList ->
            assertEquals("custom message", errorList.message)
        }
    }

    @Test
    fun inherited_failed_builder_methods_wrap_single_error_in_list() {
        val result = Validations.invalid<String>("bad input")

        assertEquals(false, result.success)
        result.onFailure { errorList ->
            assertEquals(1, errorList.errors.size)
            assertEquals("bad input", errorList.errors.single().message)
        }
    }

    @Test
    fun error_from_err_does_not_double_wrap_existing_error_list() {
        val original = Err.ErrorList(listOf(Err.of("first"), Err.of("second")), "multiple")
        val result = Validations.invalid<String>(original)

        result.onFailure { errorList ->
            assertEquals(original, errorList)
        }
    }
}
