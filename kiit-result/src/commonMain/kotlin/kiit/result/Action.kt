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
@file:JvmName("Actions")
@file:OptIn(ExperimentalJsExport::class)

package kiit.result

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * Describes the operation that produced a [Result] — the "what were we doing" complement to
 * [kiit.codes.Status] (the kind of outcome) and the error/value payload (the detail). Attach via
 * [withAction]; supported on both [Success] and [Failure], though it's most useful on failures for
 * pinpointing where in a call chain something went wrong.
 *
 * @param action : Required name of the operation (e.g. "chargeCard")
 * @param xid : Optional correlation id for tracing this operation across systems/logs
 * @param data : Optional free-form attributes for this operation
 * @param previous : Optional link to the [Action] this one was chained from, see [withAction]
 */
@JsExport
data class Action
    @JvmOverloads
    constructor(
        val action: String,
        val xid: String? = null,
        val data: Map<String, String> = mapOf(),
        val previous: Action? = null,
    )

/**
 * Applies the supplied [Action] to this result.
 *
 * @param action : The [Action] describing the operation that produced/wrapped this result. If it
 *                 already has an explicit [Action.previous] set, that's always respected.
 * @param chain : When true (default) and [action] has no explicit [Action.previous], links it to
 *                whatever action is already on this result, preserving history across nested
 *                operations. When false, [action] is applied as-is.
 *
 * # Example
 * ```
 * Success(42).withAction(Action("chargeCard", xid = "req-123"))
 * ```
 */
@JsExport
@Suppress("NOTHING_TO_INLINE")
@JvmOverloads
inline fun <T, E> Result<T, E>.withAction(action: Action, chain: Boolean = true): Result<T, E> {
    val next = if (chain) action.copy(previous = action.previous ?: this.action) else action
    return when (this) {
        is Success -> this.copy(action = next)
        is Failure -> this.copy(action = next)
    }
}
