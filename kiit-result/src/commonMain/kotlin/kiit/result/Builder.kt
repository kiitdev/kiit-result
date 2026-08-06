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
import kiit.codes.Succeeded
import kiit.codes.Unserved

/**
 * Provides convenient ways to build the most common Successes/Failures with
 * customizable error type [E] with support for [String], [Exception], [Err] as error type [E].
 * Composed from [PassedBuilder] and [FailedBuilder]; [of] lives here rather than on either half
 * since it calls into both `success(...)` and `unserved(...)`.
 */
interface Builder<out E> : PassedBuilder<E>, FailedBuilder<E> {
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
