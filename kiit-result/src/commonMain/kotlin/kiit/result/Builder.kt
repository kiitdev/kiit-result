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
import kiit.codes.Excluded
import kiit.codes.Failed
import kiit.codes.Invalid
import kiit.codes.Passed
import kiit.codes.Pending
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.Status
import kiit.codes.Succeeded
import kiit.codes.Unserved

/**
 * Provides convenient ways to build the most common Successes/Failures with
 * customizable error type [E] with support for [String], [Exception], [Err] as error type [E]
 */
interface Builder<out E> {
    /**
     * Build the error type [E] from a [Throwable]
     */
    fun errorFromEx(ex: Throwable, defaultStatus: Status): E

    /**
     * Build the error type [E] from a [String]
     */
    fun errorFromStr(msg: String?, defaultStatus: Status): E

    /**
     * Build the error type [E] from a [Err]
     */
    fun errorFromErr(err: Err, defaultStatus: Status): E

    // The success(...) methods below could be 100% replaced with direct usage of top level class Success
    // But it's here for completeness to be able to build all the various types
    // of successes / failures using builder methods.
    fun <T> success(): Result<T?, E> = Success(null)

    fun <T> success(value: T): Result<T, E> = Success(value)

    fun <T> success(value: T, msg: String): Result<T, E> = Success(value, msg)

    fun <T> success(value: T, status: Passed.Succeeded): Result<T, E> = Success(value, status)

    fun <T> pending(): Result<T?, E> = Success(null, status = Pending.ACCEPTED)

    fun <T> pending(value: T): Result<T, E> = Success(value, Pending.ACCEPTED)

    fun <T> pending(value: T, msg: String): Result<T, E> = Success(value, Status.ofStatus(msg, null, Pending.ACCEPTED))

    fun <T> pending(value: T, status: Passed.Pending): Result<T, E> = Success(value, status)

    // An excluded item is modeled as a Success — see kiit-codes' Passed.Excluded. An item that was
    // intentionally left out of an operation (deduplicated, disqualified, skipped by a filter)
    // isn't a failure, so this builds a [Success], not a [Failure].
    fun <T> excluded(): Result<T?, E> = Success(null, status = Excluded.SKIPPED)

    fun <T> excluded(value: T): Result<T, E> = Success(value, Excluded.SKIPPED)

    fun <T> excluded(value: T, msg: String): Result<T, E> = Success(value, Status.ofStatus(msg, null, Excluded.SKIPPED))

    fun <T> excluded(value: T, status: Passed.Excluded): Result<T, E> = Success(value, status)

    fun <T> restricted(): Result<T, E> = Failure(errorFromStr(null, Restricted.DENIED), Restricted.DENIED)

    fun <T> restricted(msg: String): Result<T, E> = Failure(errorFromStr(msg, Restricted.DENIED), Restricted.DENIED)

    fun <T> restricted(ex: Throwable, status: Failed.Restricted? = null): Result<T, E> =
        Failure(errorFromEx(ex, Restricted.DENIED), status ?: Restricted.DENIED)

    fun <T> restricted(err: Err, status: Failed.Restricted? = null): Result<T, E> =
        Failure(errorFromErr(err, Restricted.DENIED), status ?: Restricted.DENIED)

    fun <T> restricted(status: Failed.Restricted): Result<T, E> =
        Failure(errorFromStr(null, get(status, Restricted.DENIED)), status)

    fun <T> invalid(): Result<T, E> = Failure(errorFromStr(null, Invalid.INVALID_VALUE), Invalid.INVALID_VALUE)

    fun <T> invalid(msg: String): Result<T, E> =
        Failure(errorFromStr(msg, Invalid.INVALID_VALUE), Invalid.INVALID_VALUE)

    fun <T> invalid(ex: Throwable, status: Failed.Invalid? = null): Result<T, E> =
        Failure(errorFromEx(ex, Invalid.INVALID_VALUE), status ?: Invalid.INVALID_VALUE)

    fun <T> invalid(err: Err, status: Failed.Invalid? = null): Result<T, E> =
        Failure(errorFromErr(err, Invalid.INVALID_VALUE), status ?: Invalid.INVALID_VALUE)

    fun <T> invalid(status: Failed.Invalid): Result<T, E> =
        Failure(errorFromStr(null, get(status, Invalid.INVALID_VALUE)), status)

    fun <T> conflict(): Result<T, E> = Failure(errorFromStr(null, Rejected.CONFLICT), Rejected.CONFLICT)

    fun <T> conflict(msg: String): Result<T, E> = Failure(errorFromStr(msg, Rejected.CONFLICT), Rejected.CONFLICT)

    fun <T> conflict(ex: Throwable, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromEx(ex, Rejected.CONFLICT), status ?: Rejected.CONFLICT)

    fun <T> conflict(err: Err, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromErr(err, Rejected.CONFLICT), status ?: Rejected.CONFLICT)

    fun <T> conflict(status: Failed.Rejected): Result<T, E> = Failure(errorFromStr(null, Rejected.CONFLICT), status)

    // General purpose "the caller was allowed, but the business refuses it" error, but allow user
    // to supply the status optionally. Mirrors kiit-codes' Failed.Rejected category.
    fun <T> rejected(): Result<T, E> =
        Failure(errorFromStr(null, get(null, Rejected.RULE_VIOLATION)), Rejected.RULE_VIOLATION)

    fun <T> rejected(msg: String, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromStr(msg, get(status, Rejected.RULE_VIOLATION)), status ?: Rejected.RULE_VIOLATION)

    fun <T> rejected(ex: Throwable, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromEx(ex, get(status, Rejected.RULE_VIOLATION)), status ?: Rejected.RULE_VIOLATION)

    fun <T> rejected(err: Err, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromErr(err, get(status, Rejected.RULE_VIOLATION)), status ?: Rejected.RULE_VIOLATION)

    fun <T> rejected(status: Failed.Rejected): Result<T, E> =
        Failure(errorFromStr(null, get(status, Rejected.RULE_VIOLATION)), status)

    // General purpose "the system can't serve it right now" error, but allow user to supply the
    // status optionally. Mirrors kiit-codes' Failed.Unserved category. Named unserved to avoid
    // collision with Kotlin's own [Result.failed].
    fun <T> unserved(): Result<T, E> = Failure(errorFromStr(null, Unserved.UNEXPECTED), Unserved.UNEXPECTED)

    fun <T> unserved(msg: String): Result<T, E> = Failure(errorFromStr(msg, Unserved.UNEXPECTED), Unserved.UNEXPECTED)

    fun <T> unserved(ex: Throwable, status: Failed.Unserved? = null): Result<T, E> =
        Failure(errorFromEx(ex, Unserved.UNEXPECTED), status ?: Unserved.UNEXPECTED)

    fun <T> unserved(err: Err, status: Failed.Unserved? = null): Result<T, E> =
        Failure(errorFromErr(err, Unserved.UNEXPECTED), status ?: Unserved.UNEXPECTED)

    fun <T> unserved(status: Failed.Unserved): Result<T, E> =
        Failure(errorFromStr(null, get(status, Unserved.UNEXPECTED)), status)

    /**
     * Builds a status from a message.
     * NOTE: This is a minor optimization where there is no need to create a status from the
     * message if the message is null / empty.
     */
    fun get(status: Status?, defaultStatus: Status): Status = status ?: defaultStatus

    /**
     * Build a Result<T,E> using the supplied condition and default error builders
     */
    fun <T> of(
        condition: Boolean,
        t: T?,
        err: String? = null,
        success: Passed.Succeeded = Succeeded.SUCCESS,
        failure: Failed.Unserved = Unserved.UNEXPECTED,
    ): Result<T, E> {
        return if (!condition) {
            unserved(failure)
        } else if (t == null) {
            unserved(failure)
        } else {
            success(t, success)
        }
    }

    /**
     * Build a Result<T,E> for a possible null value
     */
    fun <T> of(t: T?): Result<T, E> =
        when (t) {
            null -> unserved("null")
            else -> success(t)
        }
}
