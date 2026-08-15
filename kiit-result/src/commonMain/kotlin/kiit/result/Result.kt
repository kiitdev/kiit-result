/**
 *  <kiit_header>
 * url: www.kiit.dev
 * git: www.github.com/slatekit/kiit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 * about: A Kotlin Tool-Kit for Server + Android
 *  </kiit_header>
 */
@file:JvmName("Results")
@file:OptIn(ExperimentalJsExport::class, ExperimentalJsStatic::class)

package kiit.result

import kiit.codes.Err
import kiit.codes.Failed
import kiit.codes.Passed
import kiit.codes.Status
import kiit.codes.Succeeded
import kiit.codes.Unserved
import kiit.codes.toException
import kotlin.js.ExperimentalJsExport
import kotlin.js.ExperimentalJsStatic
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.JsStatic
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Models successes and failures with an optional status.
 * Similar to Result in Rust and Swift, or Try in Scala.
 *
 * DESIGN
 * While similar to other implementations, there are a few major differences:
 * 1. Flexible error   : Error type on the Failure branch can be anything, Exception, Err, String.
 * 2. Status taxonomy  : Every branch carries a status from kiit-codes' closed group taxonomy.
 * 3. Sensible defaults: Builders in `kiit.result.builders` supply a default status per group
 *                       so routine use rarely needs to construct a [Status] by hand.
 *
 * NOTES:
 * 1. The success value is of type T
 * 2. The failure value is of type E
 * 3. The status is optional to specify explicitly, it's always initialized, defaulting per branch
 */
@JsExport
sealed class Result<out T, out E> {
    /**
     * Optional status is defaulted in the [Success] and [Failure]
     * branches using the built-in codes from kiit-codes
     */
    abstract val status: Status

    /**
     * Optional [Action] describing the operation that produced/wrapped this result.
     * Absent (null) unless explicitly attached via [withAction].
     */
    abstract val action: Action?

    /**
     * These are here for convenience both internally and externally
     */
    val success: Boolean get() = this is Success
    val message: String get() = status.message

    /**
     * Applies supplied function `f` if this is a [Success]
     *
     * @param f: the function to apply
     *
     * # Example
     * ```
     * val r1 = Success("Superman").map { "Clark Kent" }  // Success("Clark Kent")
     * val r2 = Failure("Unknown" ).map { "???"        }  // Failure("Unknown")
     * ```
     */
    inline fun <T2> map(f: (T) -> T2): Result<T2, E> =
        when (this) {
            is Success -> Success(f(this.value), this.status, this.action)
            is Failure -> this
        }

    /**
     * Applies supplied function `f` if this is a [Failure] to transform the error type
     *
     * @param f: the function to apply
     *
     * # Example
     * ```
     * val r1 = Success("Superman").mapError { "unknown" }  // Success("Superman")
     * val r2 = Failure("error"    ).mapError { "unknown" }  // Failure("unknown")
     * ```
     */
    inline fun <E2> mapError(f: (E) -> E2): Result<T, E2> =
        when (this) {
            is Success -> this
            is Failure -> Failure(f(this.error), this.status, this.action)
        }

    /**
     * Applies `onSuccess` if this is a [Success] or `onError` if this a [Failure]
     *
     * @param onSuccess: The function to apply if this is a [Success]
     * @param onFailure: The function to apply if this is a [Failure]
     *
     * # Example:
     * ```
     * val result : Result<String,Err> = someOperation()
     * result.fold(
     *      { println( "operation succeeded" ) },
     *      { println( "operation failed"    ) }
     * )
     * ```
     */
    inline fun <T2> fold(onSuccess: (T) -> T2, onFailure: (E) -> T2): T2 {
        return when (this) {
            is Success -> onSuccess(this.value)
            is Failure -> onFailure(this.error)
        }
    }

    /**
     * Returns the result of supplied function `f` if this is a [Success], or false otherwise
     *
     * @param f: the function to apply
     *
     * # Example
     * ```
     * Success(42).exists { it >= 42 } // true
     * Success(40).exists { it >= 42 } // false
     * ```
     */
    inline fun exists(f: (T) -> Boolean): Boolean =
        when (this) {
            is Success -> f(this.value)
            is Failure -> false
        }

    /**
     * Returns the value from this [Success] or null if this is a [Failure]
     *
     * # Example
     * ```
     * Success(42).getOrNull  // 42
     * Failure(40).getOrNull  // null
     * ```
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun getOrNull(): T? =
        when (this) {
            is Success -> this.value
            is Failure -> null
        }

    /**
     * Applies the supplied function if this is a [Success]
     *
     * @param f: the function to apply
     *
     * # Example
     * ```
     * Success(42).onSuccess { println(it) } // 42
     * Failure(40).onSuccess { println(it) } // function not applied
     * ```
     */
    inline fun onSuccess(f: (T) -> Unit): Result<T, E> =
        when (this) {
            is Success -> {
                f(this.value)
                this
            }

            is Failure -> this
        }

    /**
     * Applies the supplied function if this is a [Failure]
     *
     * @param f: the function to apply
     *
     * # Example
     * ```
     * Success(42).onFailure { println(it) } // function not applied
     * Failure(40).onFailure { println(it) } // 40
     * ```
     */
    inline fun onFailure(f: (E) -> Unit): Result<T, E> =
        when (this) {
            is Success -> this
            is Failure -> {
                f(this.error)
                this
            }
        }

    /**
     * Applies the supplied functions to transform this Result
     *
     * @param onSuccess: The function to apply for a [Success]
     * @param onFailure: The function to apply for a [Failure]
     *
     * # Example
     * ```
     * Success(42).transform( { "$it" }, { "error"} ) // Result<String,E>
     * ```
     */
    inline fun <T2, E2> transform(onSuccess: (T) -> Result<T2, E2>, onFailure: (E) -> Result<T2, E2>): Result<T2, E2> =
        when (this) {
            is Success -> onSuccess(this.value)
            is Failure -> onFailure(this.error)
        }

    /**
     * Applies the supplied status to this result
     *
     * @param passedStatus: The [Passed] status to apply if success
     * @param failedStatus: The [Failed] status to apply if failure
     *
     * # Example
     * ```
     * Success(42).withStatus( Succeeded.CREATED, Restricted.DENIED ) // Result<Int,E>
     * ```
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun withStatus(passedStatus: Passed, failedStatus: Failed): Result<T, E> =
        when (this) {
            is Success -> this.copy(status = passedStatus)
            is Failure -> this.copy(status = failedStatus)
        }

    /**
     * Transform this to an Outcome (type alias ) with error type of [Err]
     *
     * # Example
     * ```
     * val err1:Result<Int,String> = Failure("Some error")
     * val err2:Result<Int,Err>    = err1.toOutcome()
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun toOutcome(retainStatus: Boolean = true): Outcome<T> =
        when (this) {
            is Success -> this
            is Failure -> {
                val err =
                    when (this.error) {
                        null -> Err.of(Unserved.UNEXPECTED)
                        is Err -> error
                        is String -> Err.of(error)
                        is Exception -> Err.ex(error)
                        else -> Err.obj(error)
                    }
                when (retainStatus) {
                    false -> Failure(err, Unserved.UNEXPECTED, this.action)
                    true -> Failure(err, this.status, this.action)
                }
            }
        }

    /**
     * Transform this to a Try (type alias ) with error type of [Exception]
     *
     * # Example
     * ```
     * val err1:Result<Int,String> = Failure("Some error")
     * val err2:Result<Int,Exception> = err1.toTry()
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun toTry(): Try<T> =
        when (this) {
            is Success -> this
            is Failure -> {
                when (this.error) {
                    is Exception -> this as Try<T>
                    is Err -> Failure(this.status.toException(listOf(this.error)), this.status, this.action)
                    null -> Failure(Exception(this.status.message), this.status, this.action)
                    else -> Failure(Exception(this.error.toString()), this.status, this.action)
                }
            }
        }

    companion object {
        @JvmStatic
        @JsStatic
        fun <T> attempt(f: () -> T): Try<T> = Tries.attempt(f)

        @JvmStatic
        @JsStatic
        fun <T> outcome(f: () -> T): Outcome<T> = Outcomes.attempt(f)
    }
}

/**
 * Success branch of the Result
 *
 * @param value : Value representing the success
 * @param status : Optional status as [Passed]
 *
 * Java note: `Result<out T, out E>`'s covariance and the `Nothing` in `Result<T, Nothing>` below
 * are Kotlin-only concepts with no fix possible for Java. `out` shows up as wildcard-bounded
 * generics (`Result<? extends T, ? extends E>`), and `Nothing` as an uninstantiable type
 * parameter. Not a bug, just an unavoidable friction point, documented rather than "solved," same
 * treatment kiit-codes gave its own unfixable interop frictions (typealias erasure, `KtList`).
 *
 * JS/TS note: covariance erases cleanly to plain TS generics. Extension functions (`flatMap`,
 * `getOrElse`, etc.) export as flat top-level functions taking the receiver as an explicit first
 * parameter, and the `@JsName`d secondary constructors below become `static` factory methods
 * (`Success.of(42)`, `Success.ofMessage(42, "message")`), not `new Success(42)`.
 */
@JsExport
data class Success<out T>
    @JvmOverloads
    constructor(
        val value: T,
        override val status: Passed,
        override val action: Action? = null,
    ) : Result<T, Nothing>() {
        // NOTE: These overloads are here for convenience + Java Interoperability

        /**
         * Initialize using just the value, defaulting to a [Succeeded.SUCCESS] status.
         * @param value : Value representing the success
         */
        @JsName("of")
        constructor(value: T) : this(value, Succeeded.SUCCESS)

        /**
         * Initialize using explicitly supplied message
         * @param value : Value representing the success
         * @param message : Optional message for the status
         */
        @JsName("ofMessage")
        constructor(value: T, message: String) : this(value, Status.ofStatus(message, null, Succeeded.SUCCESS))
    }

/**
 * Failure branch of the result
 *
 * @param error : Error representing the failure
 * @param status : Optional status as [Failed]
 */
@JsExport
data class Failure<out E>
    @JvmOverloads
    constructor(
        val error: E,
        override val status: Failed,
        override val action: Action? = null,
    ) : Result<Nothing, E>() {
        // NOTE: These overloads are here for convenience + Java Interoperability

        /**
         * Initialize using just the error, defaulting to an [Unserved.UNEXPECTED] status.
         * @param error : Error representing the failure
         */
        @JsName("of")
        constructor(error: E) : this(error, Unserved.UNEXPECTED)

        /**
         * Initialize using explicitly supplied message
         * @param error : Error representing the failure
         * @param message : Optional message for the status
         */
        @JsName("ofMessage")
        constructor(error: E, message: String) : this(error, Status.ofStatus(message, null, Unserved.UNEXPECTED))
    }

/**
 * Applies supplied function `f` if this is a [Success]
 *
 * @param f: the function to apply
 *
 * # Example
 * ```
 * val r1 = Success("Superman").flatMap { Success("Clark Kent") }  // Success("Clark Kent")
 * val r2 = Failure("Unknown" ).flatMap { Success("???")        }  // Failure("Unknown")
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
 * val r1 = Success("Superman").then { Success("Clark Kent") }  // Success("Clark Kent")
 * val r2 = Failure("Unknown" ).then { Success("???")        }  // Failure("Unknown")
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
 * val r1 = Success("Superman").or(Success("Clark Kent"))  // Success("Superman")
 * val r2 = Failure("Unknown" ).or(Success("Clark Kent"))  // Success("Clark Kent")
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
 * Applies supplied function `op` to this whole [Result] if this is a [Success]. Unlike
 * [flatMap]/[then], which apply to just the success value, `op` here receives the [Result] itself
 *
 * @param op: The function to apply to this [Result] if this is a [Success]
 *
 * # Example
 * ```
 * val r1 = Success("Superman").operate { it }  // Success("Superman")
 * val r2 = Failure("Unknown" ).operate { it }  // Failure("Unknown")
 * ```
 */
@JsExport
inline fun <T1, T2, E> Result<T1, E>.operate(op: (Result<T1, E>) -> Result<T2, E>): Result<T2, E> {
    return when (this) {
        is Success -> op(this)
        is Failure -> this
    }
}

/**
 * Applies supplied function `f` if this is a [Failure] to transform the error type
 *
 * @param f: the function to apply
 *
 * # Example
 * ```
 * val r1 = Success("Superman").flatMapError { "Clark Kent" }  // Success("Clark Kent")
 * val r2 = Failure("Unknown" ).flatMapError { "???"        }  // Failure("???")
 * ```
 */
@JsExport
inline fun <T, E, E2> Result<T, E>.flatMapError(f: (E) -> Result<T, E2>): Result<T, E2> =
    when (this) {
        is Success -> this
        is Failure -> f(this.error)
    }

/**
 * Returns the value from this [Success] or the default value supplied if [Failure]
 *
 * # Example
 * ```
 * Success("Superman").getOrElse("???")  // "Superman"
 * Failure("Unknown" ).getOrElse("???")  // "???"
 * ```
 */
@JsExport
inline fun <T, E> Result<T, E>.getOrElse(f: () -> T): T =
    when (this) {
        is Success -> this.value
        is Failure -> f()
    }

/**
 * Gets the inner value in a nested Result
 *
 * # Example
 * ```
 * val r1 = Success(Success("Superman")).inner() // Success("Clark Kent")
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<Result<T, E>, E>.inner(): Result<T, E> = this.fold({ it }, { Failure(it) })

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
