package sample

import kiit.codes.Failed
import kiit.codes.HasStatus
import kiit.codes.Invalid
import kiit.codes.Passed
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.result.Outcome
import kiit.result.OutcomeBuilder
import kiit.result.Result
import kiit.result.flatMap

data class User(val id: String, val email: String)

// Custom domain codes for createTyped below, each paired with its kiit-codes group via HasStatus.
private val USER_CREATED = Passed.Succeeded(name = "USER_CREATED", message = "User account created", origin = "sample")
private val EMAIL_TAKEN = Failed.Rejected(name = "EMAIL_TAKEN", message = "Email is already registered", origin = "sample")
private val INVALID_EMAIL = Failed.Invalid(name = "INVALID_EMAIL", message = "Email format is invalid", origin = "sample")
private val UNAUTHORIZED_CREATE =
    Failed.Restricted(name = "UNAUTHORIZED_CREATE", message = "Not authorized to create users", origin = "sample")

// Sealed domain success: every way createTyped can succeed.
sealed class CreateUserSuccess(override val status: Passed) : HasStatus<Passed> {
    data class Created(val user: User) : CreateUserSuccess(USER_CREATED)
}

// Sealed domain error: every way createTyped can fail, see the docs' Guide > Domain Errors.
sealed class CreateUserError(override val status: Failed) : HasStatus<Failed> {
    data class EmailTaken(val email: String) : CreateUserError(EMAIL_TAKEN)
    data class InvalidEmail(val email: String) : CreateUserError(INVALID_EMAIL)
    data object Unauthorized : CreateUserError(UNAUTHORIZED_CREATE)
}

/**
 * A tiny service that returns an [Outcome] (Result<T, Err> from kiit-result) for every operation
 * instead of throwing for expected failures. Status groups come from kiit-codes.
 */
class UserService : OutcomeBuilder {
    private val users = mutableMapOf<String, User>()

    fun create(id: String, email: String): Outcome<User> {
        if (email.isBlank()) return invalid(Invalid.BAD_REQUEST)
        if (users.containsKey(id)) return rejected(Rejected.CONFLICT)
        val user = User(id, email)
        users[id] = user
        return success(user)
    }

    fun fetch(id: String): Outcome<User> = users[id]?.let { success(it) } ?: invalid(Invalid.NOT_FOUND)

    fun authorize(id: String, requesterId: String): Outcome<User> =
        fetch(id).flatMap { user ->
            if (user.id != requesterId) restricted(Restricted.UNAUTHORIZED) else success(user)
        }

    // Same operation as create, but with a domain-specific Result<CreateUserSuccess, CreateUserError>
    // instead of Outcome<User> (Result<User, Err>), see the docs' Guide > Domain Errors, Approach 1.
    //
    // Both success/failure calls below are fully qualified: UserService already implements
    // OutcomeBuilder, whose inherited success(value, message) member (scoped to E = Err) would
    // otherwise shadow the top-level HasStatus-based kiit.result.success used here.
    fun createTyped(id: String, email: String, isAuthorized: Boolean): Result<CreateUserSuccess, CreateUserError> =
        when {
            !isAuthorized -> kiit.result.failure(CreateUserError.Unauthorized)
            !email.contains("@") -> kiit.result.failure(CreateUserError.InvalidEmail(email))
            users.containsKey(id) -> kiit.result.failure(CreateUserError.EmailTaken(email))
            else -> {
                val user = User(id, email)
                users[id] = user
                kiit.result.success(CreateUserSuccess.Created(user))
            }
        }
}
