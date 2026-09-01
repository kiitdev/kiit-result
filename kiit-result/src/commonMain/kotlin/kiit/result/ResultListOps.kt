/** url: www.kiit.dev */
@file:JvmName("ResultLists")
@file:OptIn(ExperimentalJsExport::class)

package kiit.result

import kiit.codes.Succeeded
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmName

/*
 * Operators on List<Result<T, E>>, necessarily extension functions since Kotlin can't add members
 * to List itself. Kept in a separate file from ResultOps.kt because the receiver here is a
 * collection of Results, not a Result itself.
 */

/**
 * Sequences this list of [Result]s into one: a [Success] wrapping every value if all items
 * succeeded, or the first [Failure] encountered, short-circuiting with its status/action
 * preserved unchanged.
 *
 * When every item succeeds, the combined [Success] can't losslessly represent N individual
 * statuses, so it defaults to [Succeeded.SUCCESS], carrying the first item's [Action] (override
 * with [withAction] if a different one is needed).
 *
 * # Example
 * ```
 * listOf(Success(1), Success(2), Success(3)).combine()      // Success([1, 2, 3])
 * listOf(Success(1), Failure("boom"), Success(3)).combine() // Failure("boom")
 * ```
 */
@JsExport
fun <T, E> List<Result<T, E>>.combine(): Result<List<T>, E> {
    val values = ArrayList<T>(this.size)
    for (result in this) {
        when (result) {
            is Success -> values.add(result.value)
            is Failure -> return result
        }
    }
    return Success(values, Succeeded.SUCCESS, this.firstOrNull()?.action)
}

/**
 * Splits this list into its [Success] values and [Failure] errors, in order. Unlike [combine],
 * this doesn't construct a new [Result], so no status decision is needed.
 *
 * # Example
 * ```
 * listOf(Success(1), Failure("boom"), Success(3)).partition()  // ([1, 3], ["boom"])
 * ```
 */
@JsExport
fun <T, E> List<Result<T, E>>.partition(): Pair<List<T>, List<E>> {
    val values = ArrayList<T>()
    val errors = ArrayList<E>()
    for (result in this) {
        when (result) {
            is Success -> values.add(result.value)
            is Failure -> errors.add(result.error)
        }
    }
    return values to errors
}

/**
 * Returns true if every item in this list is a [Success].
 */
@JsExport
fun <T, E> List<Result<T, E>>.allSuccess(): Boolean = this.all { it.success }

/**
 * Returns true if every item in this list is a [Failure].
 */
@JsExport
fun <T, E> List<Result<T, E>>.allFailure(): Boolean = this.none { it.success }

/**
 * Returns true if at least one item in this list is a [Success].
 */
@JsExport
fun <T, E> List<Result<T, E>>.anySuccess(): Boolean = this.any { it.success }

/**
 * Returns true if at least one item in this list is a [Failure].
 */
@JsExport
fun <T, E> List<Result<T, E>>.anyFailure(): Boolean = this.any { !it.success }
