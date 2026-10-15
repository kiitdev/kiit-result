/** url: www.kiit.dev */

@file:OptIn(ExperimentalJsExport::class)

package kiit.result.builders

import kiit.codes.Excluded
import kiit.codes.Information
import kiit.codes.Passed
import kiit.codes.Pending
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

    @JsName("successStatus")
    fun <T> success(value: T, status: Passed.Succeeded = Succeeded.DEFAULT): Result<T, E> = Success(value, status)

    fun <T> pending(): Result<T?, E> = Success(null, status = Pending.DEFAULT)

    @JsName("pendingStatus")
    fun <T> pending(value: T, status: Passed.Pending = Pending.DEFAULT): Result<T, E> = Success(value, status)

    // An excluded item is modeled as a Success, see kiit-codes' Passed.Excluded. An item that
    // was intentionally left out of an operation (deduplicated, disqualified, skipped by a
    // filter) isn't a failure, so this builds a [Success], not a [Failure].
    fun <T> excluded(): Result<T?, E> = Success(null, status = Excluded.DEFAULT)

    @JsName("excludedStatus")
    fun <T> excluded(value: T, status: Passed.Excluded = Excluded.DEFAULT): Result<T, E> = Success(value, status)

    fun <T> information(): Result<T?, E> = Success(null, status = Information.DEFAULT)

    @JsName("informationStatus")
    fun <T> information(value: T, status: Passed.Information = Information.DEFAULT): Result<T, E> = Success(value, status)
}
