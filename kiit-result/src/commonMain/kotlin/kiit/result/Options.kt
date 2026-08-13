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
import kiit.codes.Rejected
import kiit.codes.Status
import kiit.codes.Unserved
import kiit.result.builders.Builder
import kotlin.jvm.JvmStatic

/**
 * Builds [Result] with [Failure] error type of [Unit]
 */
interface OptionsBuilder : Builder<Unit> {
    override fun errorFromEx(ex: Throwable, defaultStatus: Status): Unit = Unit

    override fun errorFromStr(msg: String?, defaultStatus: Status): Unit = Unit

    override fun errorFromErr(err: Err, defaultStatus: Status): Unit = Unit
}

/**
 * Builds [Result] with [Failure] error type of [Unit]. `Option<T>` intentionally mirrors the
 * historical role of `Option`/`Maybe` in Rust/Scala/Arrow — standing in for a nullable value — but
 * built on the same [Result] as everything else here, so absence can carry a [status] explaining
 * *why* instead of being a bare `None`. [some]/[none] are the discoverable entry points for that;
 * every other [Builder] method (`success`, `restricted`, etc.) still works identically on `Option`.
 */
object Options : OptionsBuilder {
    /**
     * Build an Option<T> ( type alias ) for Result<T,Unit> using the supplied function
     */
    @JvmStatic
    inline fun <T> of(f: () -> T): Option<T> = build(f, { _ -> Unserved.UNEXPECTED })

    /**
     * Build a Result<T,E> using the supplied callback and error handler
     */
    @JvmStatic
    inline fun <T> build(f: () -> T, onError: (Throwable) -> Failed): Option<T> =
        try {
            val data = f()
            Success(data)
        } catch (e: Throwable) {
            val status = onError(e)
            Failure(Unit, status)
        }

    /**
     * Builds a present [Option] — the [Result] equivalent of Rust/Scala's `Some`.
     */
    @JvmStatic
    fun <T> some(value: T): Option<T> = success(value)

    /**
     * Builds an absent [Option] — the [Result] equivalent of Rust/Scala's `None`, with a [status]
     * explaining why the value is absent (defaults to [Rejected.NOT_EXISTS]).
     */
    @JvmStatic
    fun <T> none(): Option<T> = Failure(Unit, Rejected.NOT_EXISTS)

    @JvmStatic
    fun <T> none(msg: String): Option<T> = Failure(Unit, Status.ofStatus(msg, null, Rejected.NOT_EXISTS))

    @JvmStatic
    fun <T> none(status: Failed.Rejected): Option<T> = Failure(Unit, status)
}
