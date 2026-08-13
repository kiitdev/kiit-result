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
import kiit.codes.Invalid
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.Status
import kiit.codes.StatusException
import kiit.codes.Unserved
import kiit.result.builders.Builder
import kotlin.jvm.JvmStatic

/**
 * Builds [Result] with [Failure] error type of [Throwable]
 */
interface TryBuilder : Builder<Throwable> {
    override fun errorFromEx(ex: Throwable, defaultStatus: Status): Throwable = ex

    override fun errorFromStr(msg: String?, defaultStatus: Status): Throwable = Throwable(msg ?: defaultStatus.message)

    override fun errorFromErr(err: Err, defaultStatus: Status): Throwable = Throwable(err.msg, err.cause)
}

/**
 * Builds [Result] with [Failure] error type of [Throwable]
 */
object Tries : TryBuilder {
    /**
     * Build a Try<T> ( Result<T,Throwable> ) using the supplied callback, catching any thrown
     * exception. This allows for using a thrown [kiit.codes.StatusException] to build the Try
     * by getting the appropriate status out of the thrown exception.
     */
    @JvmStatic
    inline fun <T> attempt(f: () -> T): Try<T> =
        try {
            val data = f()
            Success(data)
        } catch (e: StatusException.RestrictedException) {
            Tries.restricted(e, e.status as? Restricted)
        } catch (e: StatusException.InvalidException) {
            Tries.invalid(e, e.status as? Invalid)
        } catch (e: StatusException.RejectedException) {
            Tries.rejected(e, e.status as? Rejected)
        } catch (e: StatusException.UnservedException) {
            Tries.unserved(e, e.status as? Unserved)
        } catch (e: Throwable) {
            Tries.unserved(e)
        }
}
