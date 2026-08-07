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

package kiit.result

import kiit.codes.Err
import kiit.codes.Failed
import kiit.codes.Passed
import kiit.codes.Status
import kiit.codes.Succeeded
import kiit.codes.Unserved
import kiit.codes.toException

/**
 * Models successes and failures with an optional status.
 * Similar to Result in Rust and Swift, or Try in Scala.
 *
 * DESIGN
 * While similar to other implementations, there are a few major differences:
 * 1. Flexible error   : Error type on the Failure branch can be anything — Exception, Err, String.
 * 2. Status taxonomy  : Every branch carries a status from kiit-codes' closed category taxonomy.
 * 3. Sensible defaults: Builders in `kiit.result.builders` supply a default status per category
 *                       so routine use rarely needs to construct a [Status] by hand.
 *
 * NOTES:
 * 1. The success value is of type T
 * 2. The failure value is of type E
 * 3. The status is optional to specify explicitly — it's always initialized, defaulting per branch
 */
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
     * val r1 = Success("Superman").map { "Clark Kent" }  // Success("Clark Kent")
     * val r2 = Failure("Unknown" ).map { "???"        }  // Failure("Unknown")
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
     *
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
     *
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
     *
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
     *
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
        fun <T> attempt(f: () -> T): Try<T> = Tries.attempt(f)

        fun <T> outcome(f: () -> T): Outcome<T> = Outcomes.attempt(f)
    }
}

/**
 * Success branch of the Result
 *
 * @param value : Value representing the success
 * @param status : Optional status as [Passed]
 */
data class Success<out T>(
    val value: T,
    override val status: Passed,
    override val action: Action? = null,
) : Result<T, Nothing>() {
    // NOTE: These overloads are here for convenience + Java Interoperability

    /**
     * Initialize using explicitly supplied message
     * @param value : Value representing the success
     */
    constructor(value: T) : this(value, Succeeded.SUCCESS)

    /**
     * Initialize using explicitly supplied message
     * @param value : Value representing the success
     * @param msg : Optional message for the status
     */
    constructor(value: T, msg: String) : this(value, Status.ofStatus(msg, null, Succeeded.SUCCESS))
}

/**
 * Failure branch of the result
 *
 * @param error : Error representing the failure
 * @param status : Optional status as [Failed]
 */
data class Failure<out E>(
    val error: E,
    override val status: Failed,
    override val action: Action? = null,
) : Result<Nothing, E>() {
    // NOTE: These overloads are here for convenience + Java Interoperability

    /**
     * Initialize using explicitly supplied message
     * @param error : Error representing the failure
     */
    constructor(error: E) : this(error, Unserved.UNEXPECTED)

    /**
     * Initialize using explicitly supplied message
     * @param error : Error representing the failure
     * @param msg : Optional message for the status
     */
    constructor(error: E, msg: String) : this(error, Status.ofStatus(msg, null, Unserved.UNEXPECTED))
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
inline fun <T1, T2, E> Result<T1, E>.then(f: (T1) -> Result<T2, E>): Result<T2, E> =
    when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }

/**
 * Applies supplied function `op` if this is a [Success]. The difference to flatMap / then is that the whole
 * Result is provided as an input
 *
 * @param op: The function to apply if this is a [Success]
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

@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.or(other: (Result<T, E>)): Result<T, E> {
    return when (this) {
        is Success -> this
        is Failure -> other
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.and(other: Result<T, E>): Result<T, E> =
    when (this) {
        is Success -> other
        is Failure -> this
    }

/**
 * Applies supplied function `op` if this is a [Success]. The difference to flatMap / then is that the whole
 * Result is provided as an input
 *
 * @param op: The function to apply if this is a [Success]
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
fun <T> T.toSuccess(): Result<T, Nothing> = Success(this)

/**
 * Builds a Result as a [Failure] with the value supplied
 *
 * # Example
 * ```
 * 400.failure() // Failure(400)
 * ```
 */
fun <E> E.toFailure(): Result<Nothing, E> = Failure(this)
