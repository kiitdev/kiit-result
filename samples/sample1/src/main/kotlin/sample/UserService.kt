package sample

import kiit.codes.Invalid
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.result.Outcome
import kiit.result.OutcomeBuilder
import kiit.result.flatMap

data class User(val id: String, val email: String)

/**
 * A tiny service that returns an [Outcome] (Result<T, Err> from kiit-result) for every operation
 * instead of throwing for expected failures. Status categories come from kiit-codes.
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
}
