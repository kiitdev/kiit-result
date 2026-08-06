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

import kiit.codes.Err
import kiit.codes.Failed
import kiit.codes.Status
import kiit.codes.Unserved
import kiit.result.Failure
import kiit.result.Option
import kiit.result.Result
import kiit.result.Success

/**
 * Builds [Result] with [Failure] error type of [Unit]
 */
interface OptionsBuilder : Builder<Unit> {
    override fun errorFromEx(ex: Throwable, defaultStatus: Status): Unit = Unit

    override fun errorFromStr(msg: String?, defaultStatus: Status): Unit = Unit

    override fun errorFromErr(err: Err, defaultStatus: Status): Unit = Unit
}

/**
 * Builds [Result] with [Failure] error type of [Unit]
 */
object Options : OptionsBuilder {
    /**
     * Build an Option<T> ( type alias ) for Result<T,Unit> using the supplied function
     */
    inline fun <T> of(f: () -> T): Option<T> = build(f, { _ -> Unserved.UNEXPECTED })

    /**
     * Build a Result<T,E> using the supplied callback and error handler
     */
    inline fun <T> build(f: () -> T, onError: (Throwable) -> Failed): Option<T> =
        try {
            val data = f()
            Success(data)
        } catch (e: Throwable) {
            val status = onError(e)
            Failure(Unit, status)
        }
}
