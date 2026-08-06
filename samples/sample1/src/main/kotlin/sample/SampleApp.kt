package sample

import kiit.codes.CodesToHttp
import kiit.result.Outcome

private val http = CodesToHttp()

fun main() {
    val service = UserService()

    report("create alice", service.create("alice", "alice@example.com"))
    report("create alice again", service.create("alice", "alice@example.com"))
    report("create with blank email", service.create("bob", ""))

    report("authorize alice as alice", service.authorize("alice", "alice"))
    report("authorize alice as bob", service.authorize("alice", "bob"))
    report("authorize unknown user", service.authorize("carol", "carol"))

    // Result composition — map/onSuccess/onFailure chaining
    service.fetch("alice")
        .map { it.email }
        .onSuccess { println("alice's email: $it") }
        .onFailure { println("could not fetch alice: $it") }

    // toTry() converts a Failure<Err> into a Failure<StatusException> (from kiit-codes) so it can
    // cross a call boundary that only communicates via exceptions.
    service.fetch("missing").toTry().onFailure { ex ->
        println("caught as exception: ${ex.message}")
    }
}

private fun report(label: String, outcome: Outcome<User>) {
    val status = outcome.status
    val httpCode = http.toCode(status)
    val detail = outcome.fold({ user -> "user=${user.id}" }, { err -> "error=${err.msg}" })
    println("$label -> ${status.name} (success=${outcome.success}, http=$httpCode, $detail)")
}
