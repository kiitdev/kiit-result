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
import kiit.codes.Status
import kiit.result.builders.Builder

/**
 * Builds [Result] with [Failure] error type of [Err]
 */
interface OutcomeBuilder : Builder<Err> {
    override fun errorFromEx(ex: Throwable, defaultStatus: Status): Err = Err.ex(ex)

    override fun errorFromStr(msg: String?, defaultStatus: Status): Err = Err.of(msg ?: defaultStatus.message)

    override fun errorFromErr(err: Err, defaultStatus: Status): Err = err
}

/**
 * Builds [Result] with [Failure] error type of [Err]
 */
object Outcomes : OutcomeBuilder {
    /**
     * Build an Outcome<T> ( type alias ) for Result<T,Err> using the supplied function,
     * catching any thrown exception and adapting it into a [Failure].
     */
    inline fun <T> attempt(f: () -> T): Outcome<T> = build(f, { ex -> Err.ex(ex) })

    /**
     * Build an Outcome<T> using the supplied function, catching any thrown exception, and
     * tag the result with an [Action] describing the operation.
     *
     * # Example
     * ```
     * Outcomes.attempt("chargeCard", xid = requestId) { paymentClient.charge(amount) }
     * ```
     */
    inline fun <T> attempt(
        action: String,
        xid: String? = null,
        data: Map<String, String> = mapOf(),
        f: () -> T,
    ): Outcome<T> = attempt(f).withAction(Action(action, xid, data))

    /**
     * Run the supplied Outcome-producing operation and tag the result with an [Action]
     * describing the operation. Unlike [attempt], this does not catch exceptions — `op` is
     * expected to already return an [Outcome].
     *
     * # Example
     * ```
     * Outcomes.of("getUser", xid = requestId) { userRepo.find(id) }
     * ```
     */
    inline fun <T> of(
        action: String,
        xid: String? = null,
        data: Map<String, String> = mapOf(),
        op: () -> Outcome<T>,
    ): Outcome<T> = op().withAction(Action(action, xid, data))

    /**
     * Run the supplied Outcome-producing operation and tag the result with the given [Action].
     * Use this over the string-based [of] when you need finer control than `action`/`xid`/`data`
     * alone can express — most commonly, an explicit [Action.previous] to chain onto.
     *
     * # Example
     * ```
     * Outcomes.of(Action("justiceLeague", xid = requestId, previous = earlier.action)) {
     *     userRepo.find(id)
     * }
     * ```
     */
    inline fun <T> of(action: Action, chain: Boolean = true, op: () -> Outcome<T>): Outcome<T> =
        op().withAction(action, chain)

    /**
     * Build a Result<T,E> using the supplied callback and error handler
     */
    inline fun <T> build(f: () -> T, onError: (Throwable) -> Err): Outcome<T> =
        try {
            val data = f()
            Success(data)
        } catch (e: Throwable) {
            Failure(onError(e))
        }
}
