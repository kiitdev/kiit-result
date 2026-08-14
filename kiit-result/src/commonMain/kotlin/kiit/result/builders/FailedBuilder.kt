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

@file:OptIn(ExperimentalJsExport::class)

package kiit.result.builders

import kiit.codes.Err
import kiit.codes.Failed
import kiit.codes.Invalid
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.Status
import kiit.codes.Unserved
import kiit.result.Failure
import kiit.result.Result
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Builder methods for the [Failed] side of the taxonomy — restricted/invalid/rejected/unserved.
 * Kept separate from [PassedBuilder] so each interface's surface stays scoped to one branch.
 */
@JsExport
interface FailedBuilder<out E> {
    /**
     * Build the error type [E] from a [Throwable]
     */
    fun errorFromEx(ex: Throwable, defaultStatus: Status): E

    /**
     * Build the error type [E] from a [String]
     */
    fun errorFromStr(msg: String?, defaultStatus: Status): E

    /**
     * Build the error type [E] from an [Err]
     */
    fun errorFromErr(err: Err, defaultStatus: Status): E

    fun <T> restricted(): Result<T, E> = Failure(errorFromStr(null, Restricted.DENIED), Restricted.DENIED)

    @JsName("restrictedMessage")
    fun <T> restricted(msg: String): Result<T, E> = Failure(errorFromStr(msg, Restricted.DENIED), Restricted.DENIED)

    @JsName("restrictedFromException")
    fun <T> restricted(ex: Throwable, status: Failed.Restricted? = null): Result<T, E> =
        Failure(errorFromEx(ex, Restricted.DENIED), status ?: Restricted.DENIED)

    @JsName("restrictedFromErr")
    fun <T> restricted(err: Err, status: Failed.Restricted? = null): Result<T, E> =
        Failure(errorFromErr(err, Restricted.DENIED), status ?: Restricted.DENIED)

    @JsName("restrictedStatus")
    fun <T> restricted(status: Failed.Restricted): Result<T, E> =
        Failure(errorFromStr(null, get(status, Restricted.DENIED)), status)

    fun <T> invalid(): Result<T, E> = Failure(errorFromStr(null, Invalid.INVALID_VALUE), Invalid.INVALID_VALUE)

    @JsName("invalidMessage")
    fun <T> invalid(msg: String): Result<T, E> =
        Failure(errorFromStr(msg, Invalid.INVALID_VALUE), Invalid.INVALID_VALUE)

    @JsName("invalidFromException")
    fun <T> invalid(ex: Throwable, status: Failed.Invalid? = null): Result<T, E> =
        Failure(errorFromEx(ex, Invalid.INVALID_VALUE), status ?: Invalid.INVALID_VALUE)

    @JsName("invalidFromErr")
    fun <T> invalid(err: Err, status: Failed.Invalid? = null): Result<T, E> =
        Failure(errorFromErr(err, Invalid.INVALID_VALUE), status ?: Invalid.INVALID_VALUE)

    @JsName("invalidStatus")
    fun <T> invalid(status: Failed.Invalid): Result<T, E> =
        Failure(errorFromStr(null, get(status, Invalid.INVALID_VALUE)), status)

    // General purpose "the caller was allowed, but the business refuses it" error, but allow user
    // to supply the status optionally. Mirrors kiit-codes' Failed.Rejected category. Also covers
    // the conflict case — call rejected(status = Rejected.CONFLICT).
    fun <T> rejected(): Result<T, E> =
        Failure(errorFromStr(null, get(null, Rejected.RULE_VIOLATION)), Rejected.RULE_VIOLATION)

    @JsName("rejectedMessage")
    fun <T> rejected(msg: String, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromStr(msg, get(status, Rejected.RULE_VIOLATION)), status ?: Rejected.RULE_VIOLATION)

    @JsName("rejectedFromException")
    fun <T> rejected(ex: Throwable, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromEx(ex, get(status, Rejected.RULE_VIOLATION)), status ?: Rejected.RULE_VIOLATION)

    @JsName("rejectedFromErr")
    fun <T> rejected(err: Err, status: Failed.Rejected? = null): Result<T, E> =
        Failure(errorFromErr(err, get(status, Rejected.RULE_VIOLATION)), status ?: Rejected.RULE_VIOLATION)

    @JsName("rejectedStatus")
    fun <T> rejected(status: Failed.Rejected): Result<T, E> =
        Failure(errorFromStr(null, get(status, Rejected.RULE_VIOLATION)), status)

    // General purpose "the system can't serve it right now" error, but allow user to supply the
    // status optionally. Mirrors kiit-codes' Failed.Unserved category. Named unserved to avoid
    // collision with Kotlin's own [Result.failed].
    fun <T> unserved(): Result<T, E> = Failure(errorFromStr(null, Unserved.UNEXPECTED), Unserved.UNEXPECTED)

    @JsName("unservedMessage")
    fun <T> unserved(msg: String): Result<T, E> = Failure(errorFromStr(msg, Unserved.UNEXPECTED), Unserved.UNEXPECTED)

    @JsName("unservedFromException")
    fun <T> unserved(ex: Throwable, status: Failed.Unserved? = null): Result<T, E> =
        Failure(errorFromEx(ex, Unserved.UNEXPECTED), status ?: Unserved.UNEXPECTED)

    @JsName("unservedFromErr")
    fun <T> unserved(err: Err, status: Failed.Unserved? = null): Result<T, E> =
        Failure(errorFromErr(err, Unserved.UNEXPECTED), status ?: Unserved.UNEXPECTED)

    @JsName("unservedStatus")
    fun <T> unserved(status: Failed.Unserved): Result<T, E> =
        Failure(errorFromStr(null, get(status, Unserved.UNEXPECTED)), status)

    /**
     * Builds a status from a message.
     * NOTE: This is a minor optimization where there is no need to create a status from the
     * message if the message is null / empty.
     */
    fun get(status: Status?, defaultStatus: Status): Status = status ?: defaultStatus
}
