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

import kiit.codes.Excluded
import kiit.codes.Information
import kiit.codes.Passed
import kiit.codes.Pending
import kiit.codes.Status
import kiit.codes.Succeeded
import kiit.result.Result
import kiit.result.Success
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Builder methods for the [Passed] side of the taxonomy: success/pending/excluded. Kept separate
 * from [FailedBuilder] so each interface's surface stays scoped to one branch (mirrors how
 * kiit-codes keeps each group's constants on its own companion, not a shared object).
 */
@JsExport
interface PassedBuilder<out E> {
    // The success(...) methods below could be replaced entirely with direct usage of the top
    // level Success class, but they're here for completeness, to build every success/failure
    // shape through the same builder methods.
    fun <T> success(): Result<T?, E> = Success(null)

    @JsName("successMessage")
    fun <T> success(value: T, message: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(message, null, Succeeded.SUCCESS))

    @JsName("successStatus")
    fun <T> success(value: T, status: Passed.Succeeded): Result<T, E> = Success(value, status)

    fun <T> pending(): Result<T?, E> = Success(null, status = Pending.ACCEPTED)

    @JsName("pendingMessage")
    fun <T> pending(value: T, message: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(message, null, Pending.ACCEPTED))

    @JsName("pendingStatus")
    fun <T> pending(value: T, status: Passed.Pending): Result<T, E> = Success(value, status)

    // An excluded item is modeled as a Success, see kiit-codes' Passed.Excluded. An item that
    // was intentionally left out of an operation (deduplicated, disqualified, skipped by a
    // filter) isn't a failure, so this builds a [Success], not a [Failure].
    fun <T> excluded(): Result<T?, E> = Success(null, status = Excluded.OMITTED)

    @JsName("excludedMessage")
    fun <T> excluded(value: T, message: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(message, null, Excluded.OMITTED))

    @JsName("excludedStatus")
    fun <T> excluded(value: T, status: Passed.Excluded): Result<T, E> = Success(value, status)

    fun <T> information(): Result<T?, E> = Success(null, status = Information.NOTICE)

    @JsName("informationMessage")
    fun <T> information(value: T, message: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(message, null, Information.NOTICE))

    @JsName("informationStatus")
    fun <T> information(value: T, status: Passed.Information): Result<T, E> = Success(value, status)
}
