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

package kiit.result.builders

import kiit.codes.Excluded
import kiit.codes.Passed
import kiit.codes.Pending
import kiit.codes.Status
import kiit.codes.Succeeded
import kiit.result.Result
import kiit.result.Success

/**
 * Builder methods for the [Passed] side of the taxonomy — success/pending/excluded. Kept separate
 * from [FailedBuilder] so each interface's surface stays scoped to one branch (mirrors how
 * kiit-codes keeps each category's constants on its own companion, not a shared object).
 */
interface PassedBuilder<out E> {
    // The success(...) methods below could be 100% replaced with direct usage of top level class Success
    // But it's here for completeness to be able to build all the various types
    // of successes / failures using builder methods.
    fun <T> success(): Result<T?, E> = Success(null)

    fun <T> success(value: T, msg: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(msg, null, Succeeded.SUCCESS))

    fun <T> success(value: T, status: Passed.Succeeded): Result<T, E> = Success(value, status)

    fun <T> pending(): Result<T?, E> = Success(null, status = Pending.ACCEPTED)

    fun <T> pending(value: T, msg: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(msg, null, Pending.ACCEPTED))

    fun <T> pending(value: T, status: Passed.Pending): Result<T, E> = Success(value, status)

    // An excluded item is modeled as a Success — see kiit-codes' Passed.Excluded. An item that was
    // intentionally left out of an operation (deduplicated, disqualified, skipped by a filter)
    // isn't a failure, so this builds a [Success], not a [Failure].
    fun <T> excluded(): Result<T?, E> = Success(null, status = Excluded.SKIPPED)

    fun <T> excluded(value: T, msg: String? = null): Result<T, E> =
        Success(value, Status.ofStatus(msg, null, Excluded.SKIPPED))

    fun <T> excluded(value: T, status: Passed.Excluded): Result<T, E> = Success(value, status)
}
