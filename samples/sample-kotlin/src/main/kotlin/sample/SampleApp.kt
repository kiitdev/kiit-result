package sample

import kiit.codes.CodesToHttp
import kiit.codes.Err
import kiit.codes.Invalid
import kiit.result.Failure
import kiit.result.Outcome
import kiit.result.Outcomes
import kiit.result.Success
import kiit.result.Validated

private val http = CodesToHttp()

fun main() {
    testOutcome()
    testValidation()
    testService()
}

// Single-error validation with Outcome<T> (Result<T, Err>): picks a different status group
// depending on why the phone number failed, mirroring kiit-codes' Checked-based validatePhone,
// but built on kiit-result's Result instead.
fun validatePhone(phone: String, caller: String = "guest"): Outcome<String> {
    return when {
        phone.isEmpty() -> Outcomes.invalid(Err.on("phone", phone, "Too short"))
        phone.length > 10 -> Outcomes.invalid(Err.on("phone", phone, "Too long"))
        phone == "1111111111" -> Outcomes.rejected(Err.on("phone", phone, "Reserved phone for testing"))
        phone.startsWith("123") && caller != "admin" ->
            Outcomes.restricted(Err.on("phone", phone, "Only admins can validate internal-use numbers"))
        else -> Outcomes.success(phone)
    }
}

fun testOutcome() {
    val results =
        listOf(
            validatePhone(""),
            validatePhone("12345678901"),
            validatePhone("1111111111"),
            validatePhone("1234567890"),
            validatePhone("9876543210"),
            validatePhone("1234567890", "admin"),
        )
    results.forEach { println(it) }
}

data class UserForm(val name: String, val email: String, val phone: String)

// Multi-error validation with Validated<T> (Result<T, Err.ErrorList>): unlike validatePhone
// above, this doesn't stop at the first problem, it checks every field and reports them all
// together in one Failure.
fun validateUser(form: UserForm): Validated<UserForm> {
    val errors = mutableListOf<Err>()
    if (form.name.isBlank()) errors.add(Err.on("name", form.name, "Name is required"))
    if (!form.email.contains("@")) errors.add(Err.on("email", form.email, "Email must contain @"))
    if (form.phone.length != 10) errors.add(Err.on("phone", form.phone, "Phone must be 10 digits"))

    return if (errors.isEmpty()) {
        Success(form)
    } else {
        Failure(Err.ErrorList(errors, "Validation failed with ${errors.size} error(s)"), Invalid.INVALID_VALUE)
    }
}

fun testValidation() {
    val valid = validateUser(UserForm("Alice", "alice@example.com", "1234567890"))
    val invalid = validateUser(UserForm("", "not-an-email", "123"))

    valid.onSuccess { println("valid form: $it") }
    invalid.onFailure { errorList ->
        println("invalid form, ${errorList.errors.size} error(s):")
        errorList.errors.forEach { println("  - ${it.message}") }
    }
}

fun testService() {
    val service = UserService()

    report("create alice", service.create("alice", "alice@example.com"))
    report("create alice again", service.create("alice", "alice@example.com"))
    report("create with blank email", service.create("bob", ""))

    report("authorize alice as alice", service.authorize("alice", "alice"))
    report("authorize alice as bob", service.authorize("alice", "bob"))
    report("authorize unknown user", service.authorize("carol", "carol"))

    // Result composition: map/onSuccess/onFailure chaining
    service.fetch("alice")
        .map { it.email }
        .onSuccess { println("alice's email: $it") }
        .onFailure { println("could not fetch alice: $it") }

    // toTry() converts a Failure<Err> into a Failure<StatusException> (from kiit-codes), so it
    // can cross a call boundary that only communicates via exceptions.
    service.fetch("missing").toTry().onFailure { ex ->
        println("caught as exception: ${ex.message}")
    }
}

private fun report(label: String, outcome: Outcome<User>) {
    val status = outcome.status
    val httpCode = http.toCode(status)
    val detail = outcome.fold({ user -> "user=${user.id}" }, { err -> "error=${err.message}" })
    println("$label -> ${status.name} (success=${outcome.success}, http=$httpCode, $detail)")
}
