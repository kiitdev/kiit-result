package kiit.result

import kiit.codes.Err
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs [block] to completion without an external coroutines dependency, since kiit-result itself
 * doesn't depend on kotlinx-coroutines. Only proves a `suspend` call compiles and executes inside
 * these operators' lambdas, not concurrency or cancellation, both explicitly out of scope, see
 * the docs' Exclusions section.
 */
private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T, Throwable>? = null
    block.startCoroutine(
        Continuation(EmptyCoroutineContext) { outcome ->
            result = outcome.fold({ Success(it) }, { Failure(it) })
        },
    )
    return result!!.getOrThrow()
}

/**
 * Backs the docs' coroutine claim (Guide > Ops: Core) with an actual compiled/executed `suspend`
 * call inside each operator's lambda, not just source inspection of the `inline` modifier.
 */
class CoroutineCompositionTest {
    private suspend fun fetchEmail(id: String): String {
        if (id.isBlank()) throw IllegalArgumentException("blank id")
        return "$id@example.com"
    }

    @Test
    fun map_runs_a_suspend_call_inside_its_lambda() {
        runSuspend {
            val result = Outcomes.success("u1").map { id -> fetchEmail(id) }
            assertEquals(true, result.success)
            result.onSuccess { email -> assertEquals("u1@example.com", email) }
        }
    }

    @Test
    fun flatMap_chains_a_suspend_returning_step() {
        runSuspend {
            val result = Outcomes.success("u1").flatMap { id -> Outcomes.success(fetchEmail(id)) }
            result.onSuccess { email -> assertEquals("u1@example.com", email) }
        }
    }

    @Test
    fun onSuccess_runs_a_suspend_side_effect() {
        runSuspend {
            var captured: String? = null
            Outcomes.success("u1").onSuccess { id -> captured = fetchEmail(id) }
            assertEquals("u1@example.com", captured)
        }
    }

    @Test
    fun attempt_wraps_a_suspend_call_and_flatMap_chains_another() {
        runSuspend {
            val result = Outcomes.attempt { fetchEmail("u1") }.flatMap { email -> Outcomes.success(email.uppercase()) }
            result.onSuccess { email -> assertEquals("U1@EXAMPLE.COM", email) }
        }
    }

    @Test
    fun attempt_captures_a_thrown_exception_from_a_suspend_call() {
        runSuspend {
            val result: Outcome<String> = Outcomes.attempt { fetchEmail("") }
            assertEquals(false, result.success)
            result.onFailure { err: Err -> assertEquals("blank id", err.message) }
        }
    }
}
