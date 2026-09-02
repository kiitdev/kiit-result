package kiit.result

import kiit.codes.Failed
import kiit.codes.HasStatus
import kiit.codes.Passed
import kiit.codes.Status
import kiit.codes.Unserved
import kotlin.test.Test
import kotlin.test.assertEquals

private val USER_CREATED = Passed.Succeeded(name = "USER_CREATED", message = "User account created", origin = "users")
private val QUEUED_FOR_REVIEW =
    Passed.Pending(
        name = "QUEUED_FOR_REVIEW",
        message = "Flagged for manual review before activation",
        origin = "users",
    )

private val EMAIL_TAKEN =
    Failed.Rejected(
        name = "EMAIL_TAKEN",
        message = "Email is already registered",
        origin = "users",
    )
private val INVALID_EMAIL =
    Failed.Invalid(
        name = "INVALID_EMAIL",
        message = "Email format is invalid",
        origin = "users",
    )
private val UNAUTHORIZED_CREATE =
    Failed.Restricted(
        name = "UNAUTHORIZED_CREATE",
        message = "Not authorized to create users",
        origin = "users",
    )

private sealed class CreateUserSuccess(override val status: Passed) : HasStatus<Passed> {
    data class Created(val email: String) : CreateUserSuccess(USER_CREATED)

    data class QueuedForReview(val email: String) : CreateUserSuccess(QUEUED_FOR_REVIEW)
}

private sealed class CreateUserError(override val status: Failed) : HasStatus<Failed> {
    data class EmailTaken(val email: String) : CreateUserError(EMAIL_TAKEN)

    data class InvalidEmail(val email: String) : CreateUserError(INVALID_EMAIL)

    object Unauthorized : CreateUserError(UNAUTHORIZED_CREATE)

    data class DatabaseUnavailable(val cause: Throwable) : CreateUserError(Unserved.UNEXPECTED)
}

private fun createUser(
    email: String,
    isAuthorized: Boolean,
    existingEmails: Set<String>,
    needsReview: Boolean,
): Result<CreateUserSuccess, CreateUserError> =
    when {
        !isAuthorized -> failure(CreateUserError.Unauthorized)
        !email.contains("@") -> failure(CreateUserError.InvalidEmail(email))
        existingEmails.contains(email) -> failure(CreateUserError.EmailTaken(email))
        needsReview -> success(CreateUserSuccess.QueuedForReview(email))
        else -> success(CreateUserSuccess.Created(email))
    }

/**
 * Verifies the [HasStatus] pattern end to end, on both branches: a sealed domain-outcome
 * hierarchy where each variant carries its own custom kiit-codes status, wired into
 * [Success]/[Failure] via the [success]/[failure] helpers.
 */
class HasStatusTests {
    @Test
    fun each_domain_success_variant_carries_its_own_custom_status() {
        assertEquals(USER_CREATED, CreateUserSuccess.Created("a@b.com").status)
        assertEquals(QUEUED_FOR_REVIEW, CreateUserSuccess.QueuedForReview("a@b.com").status)
    }

    @Test
    fun each_domain_error_variant_carries_its_own_custom_status() {
        assertEquals(UNAUTHORIZED_CREATE, CreateUserError.Unauthorized.status)
        assertEquals(EMAIL_TAKEN, CreateUserError.EmailTaken("a@b.com").status)
        assertEquals(INVALID_EMAIL, CreateUserError.InvalidEmail("bad").status)
        assertEquals(Unserved.UNEXPECTED, CreateUserError.DatabaseUnavailable(RuntimeException()).status)
    }

    @Test
    fun success_helper_wires_the_domain_outcomes_own_status_into_the_result() {
        val result =
            createUser("a@b.com", isAuthorized = true, existingEmails = emptySet(), needsReview = true)
        assertEquals(true, result.success)
        assertEquals(QUEUED_FOR_REVIEW, result.status)
        result.onSuccess { outcome -> assertEquals(CreateUserSuccess.QueuedForReview("a@b.com"), outcome) }
    }

    @Test
    fun failure_helper_wires_the_domain_errors_own_status_into_the_result() {
        val result =
            createUser("bad", isAuthorized = true, existingEmails = emptySet(), needsReview = false)
        assertEquals(false, result.success)
        assertEquals(INVALID_EMAIL, result.status)
        result.onFailure { error -> assertEquals(CreateUserError.InvalidEmail("bad"), error) }
    }

    @Test
    fun unauthorized_short_circuits_before_other_checks() {
        val result =
            createUser("bad", isAuthorized = false, existingEmails = emptySet(), needsReview = false)
        assertEquals(UNAUTHORIZED_CREATE, result.status)
    }

    @Test
    fun email_taken_reported_when_authorized_and_valid_format() {
        val result =
            createUser("a@b.com", isAuthorized = true, existingEmails = setOf("a@b.com"), needsReview = false)
        assertEquals(EMAIL_TAKEN, result.status)
    }

    @Test
    fun created_directly_when_authorized_valid_not_taken_and_no_review_needed() {
        val result =
            createUser("a@b.com", isAuthorized = true, existingEmails = emptySet(), needsReview = false)
        assertEquals(true, result.success)
        assertEquals(USER_CREATED, result.status)
        result.onSuccess { outcome -> assertEquals(CreateUserSuccess.Created("a@b.com"), outcome) }
    }
}

private val ORDER_PLACED = Passed.Succeeded(name = "ORDER_PLACED", message = "Order placed", origin = "orders")
private val ORDER_OUT_OF_STOCK =
    Failed.Rejected(
        name = "ORDER_OUT_OF_STOCK",
        message = "Item is out of stock",
        origin = "orders",
    )

/**
 * A single sealed hierarchy covering both branches, instead of a separate success/error type
 * pair, paired with the [build] helper below.
 */
private sealed class PlaceOrderResult(override val status: Status) : HasStatus<Status> {
    data class Placed(val orderId: String) : PlaceOrderResult(ORDER_PLACED)

    data class OutOfStock(val sku: String) : PlaceOrderResult(ORDER_OUT_OF_STOCK)
}

private fun placeOrder(sku: String, inStock: Boolean): Result<PlaceOrderResult, PlaceOrderResult> =
    build(if (inStock) PlaceOrderResult.Placed(orderId = "ord-1") else PlaceOrderResult.OutOfStock(sku))

/**
 * Verifies the [build] helper: one combined domain type, [Success]/[Failure] inferred at runtime
 * from whichever variant's own status is [Passed] or [Failed].
 */
class HasStatusCombinedTests {
    @Test
    fun build_helper_infers_success_from_a_passed_status() {
        val result = placeOrder("sku-1", inStock = true)
        assertEquals(true, result.success)
        assertEquals(ORDER_PLACED, result.status)
        result.onSuccess { outcome -> assertEquals(PlaceOrderResult.Placed("ord-1"), outcome) }
    }

    @Test
    fun build_helper_infers_failure_from_a_failed_status() {
        val result = placeOrder("sku-1", inStock = false)
        assertEquals(false, result.success)
        assertEquals(ORDER_OUT_OF_STOCK, result.status)
        result.onFailure { outcome -> assertEquals(PlaceOrderResult.OutOfStock("sku-1"), outcome) }
    }
}
