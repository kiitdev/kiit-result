/** url: www.kiit.dev */
@file:JvmName("Results")
@file:OptIn(ExperimentalJsExport::class)

package kiit.result

import kiit.codes.Succeeded
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmName

/*
 * Operators on [Result] that must be top-level extension functions rather than members of the
 * sealed class, because [Result] is declared `Result<out T, out E>`. Kotlin's declaration-site
 * variance rules forbid a covariant class type parameter from appearing as a bare value-parameter
 * type, or being reused inside the return type of a parameter lambda, in a *member* function.
 * Every function below hits one of those two shapes:
 * - `or`/`and`/`contains`/`getOr` take `T`/`E` directly as a value parameter.
 * - `flatMap`/`then`, `orElse`, `getOrElse`, `recover` reuse `T`/`E` inside the return type of a
 *   parameter lambda.
 * - `flatten` needs a receiver narrowed to `Result<Result<T, E>, E>`, which only an extension
 *   function's receiver type can express.
 * - `getOrRethrow` needs `E` narrowed to `Throwable`, a bound the class itself doesn't declare.
 *   Same reasoning as `flatten`, just a type-parameter bound instead of a shape.
 */

/**
 * Applies supplied function `f` if this is a [Success]
 *
 * @param f: the function to apply
 *
 * # Example
 * ```
 * val r1 = Success("guest"  ).flatMap { Success("member") }  // Success("member")
 * val r2 = Failure("Unknown").flatMap { Success("???")    }  // Failure("Unknown")
 * ```
 */
@JsExport
inline fun <T1, T2, E> Result<T1, E>.flatMap(f: (T1) -> Result<T2, E>): Result<T2, E> = this.then(f)

/**
 * Applies supplied function `f` if this is a [Success]
 *
 * @param f: the function to apply
 *
 * # Example
 * ```
 * val r1 = Success("guest"  ).then { Success("member") }  // Success("member")
 * val r2 = Failure("Unknown").then { Success("???")    }  // Failure("Unknown")
 * ```
 */
@JsExport
inline fun <T1, T2, E> Result<T1, E>.then(f: (T1) -> Result<T2, E>): Result<T2, E> =
    when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }

/**
 * Returns this if it's a [Success], or the supplied fallback [other] if this is a [Failure]
 *
 * @param other: The fallback [Result] to return if this is a [Failure]
 *
 * # Example
 * ```
 * val r1 = Success("guest"  ).or(Success("member"))  // Success("guest")
 * val r2 = Failure("Unknown").or(Success("member"))  // Success("member")
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.or(other: (Result<T, E>)): Result<T, E> {
    return when (this) {
        is Success -> this
        is Failure -> other
    }
}

@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.and(other: Result<T, E>): Result<T, E> =
    when (this) {
        is Success -> other
        is Failure -> this
    }

/**
 * Applies supplied function `f` if this is a [Failure] to transform the error type. Named to
 * match Rust's `or_else` and kotlin-result's `orElse`, and to pair with [or] the same way
 * [getOrElse] pairs with [getOr].
 *
 * @param f: the function to apply
 *
 * # Example
 * ```
 * val r1 = Success("guest"  ).orElse { Success("recovered") }  // Success("guest")
 * val r2 = Failure("timeout").orElse { Success("recovered") }  // Success("recovered")
 * ```
 */
@JsExport
inline fun <T, E, E2> Result<T, E>.orElse(f: (E) -> Result<T, E2>): Result<T, E2> =
    when (this) {
        is Success -> this
        is Failure -> f(this.error)
    }

/**
 * Returns this unchanged if it's a [Success], or a new [Success] wrapping the result of applying
 * [transform] to the error if this is a [Failure]. Unlike [orElse], which can still produce
 * a [Failure], this unconditionally recovers. There's no lossless Failed→Passed status mapping,
 * so the recovered branch defaults to [Succeeded.SUCCESS], matching [Success]'s own single-value
 * constructor default. [Action] is preserved, since the operation identity continues across the
 * recovery.
 *
 * # Example
 * ```
 * Success("guest"  ).recover { "fallback" }  // Success("guest")
 * Failure("Unknown").recover { "fallback" }  // Success("fallback")
 * ```
 */
@JsExport
inline fun <T, E> Result<T, E>.recover(transform: (E) -> T): Result<T, Nothing> =
    when (this) {
        is Success -> this
        is Failure -> Success(transform(this.error), Succeeded.SUCCESS, this.action)
    }

/**
 * Returns the value from this [Success] or the result of applying `f` to the error if [Failure]
 *
 * # Example
 * ```
 * Success("guest"  ).getOrElse { "fallback" }  // "guest"
 * Failure("Unknown").getOrElse { it }          // "Unknown"
 * ```
 */
@JsExport
inline fun <T, E> Result<T, E>.getOrElse(f: (E) -> T): T =
    when (this) {
        is Success -> this.value
        is Failure -> f(this.error)
    }

/**
 * Returns the value from this [Success] or the [default] value supplied if [Failure]. Eager
 * counterpart of [getOrElse] for when the fallback doesn't need to be computed lazily.
 *
 * # Example
 * ```
 * Success("guest"  ).getOr("fallback")  // "guest"
 * Failure("Unknown").getOr("fallback")  // "fallback"
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.getOr(default: T): T =
    when (this) {
        is Success -> this.value
        is Failure -> default
    }

/**
 * Returns the value from this [Success], or rethrows the original error unchanged if this is a
 * [Failure]. Unlike [Result.getOrThrow], which always builds a new exception, this propagates
 * the existing [Throwable] as-is, same identity, same stack trace. Only available once `E` is
 * narrowed to [Throwable] (e.g. on a [Try]).
 *
 * # Example
 * ```
 * val ok: Try<Int> = Success(42)
 * ok.getOrRethrow()  // 42
 *
 * val bad: Try<Int> = Failure(IllegalStateException("boom"))
 * bad.getOrRethrow() // throws the original IllegalStateException("boom")
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E : Throwable> Result<T, E>.getOrRethrow(): T =
    when (this) {
        is Success -> this.value
        is Failure -> throw this.error
    }

/**
 * Flattens a nested [Result] into one, matching Rust's and kotlin-result's `flatten`.
 *
 * # Example
 * ```
 * val r1 = Success(Success("guest")).flatten() // Success("guest")
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<Result<T, E>, E>.flatten(): Result<T, E> = this.fold({ it }, { Failure(it) })

/**
 * Returns true if this is a [Success] with the value supplied, or false otherwise
 *
 * # Example
 * ```
 * Success(42).contains(42) // true
 * Success(40).contains(42) // false
 * Failure(39).contains(42) // false
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.contains(i: T): Boolean =
    when (this) {
        is Success -> i == this.value
        is Failure -> false
    }

/**
 * Builds a Result as a [Success] with the value supplied
 *
 * # Example
 * ```
 * 42.success() // Success(42)
 * ```
 */
@JsExport
fun <T> T.toSuccess(): Result<T, Nothing> = Success(this)

/**
 * Builds a Result as a [Failure] with the value supplied
 *
 * # Example
 * ```
 * 400.failure() // Failure(400)
 * ```
 */
@JsExport
fun <E> E.toFailure(): Result<Nothing, E> = Failure(this)
