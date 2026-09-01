/** url: www.kiit.dev */
@file:OptIn(ExperimentalJsExport::class)

package kiit.result

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

/**
 * Describes the operation that produced a [Result]: the "what were we doing" complement to
 * [kiit.codes.Status] (the kind of outcome) and the error/value payload (the detail). Attach via
 * [Result.withAction]; supported on both [Success] and [Failure], though it's most useful on
 * failures for pinpointing where in a call chain something went wrong.
 *
 * @param action : Required name of the operation (e.g. "chargeCard")
 * @param xid : Optional correlation id for tracing this operation across systems/logs
 * @param data : Optional free-form attributes for this operation
 * @param previous : Optional link to the [Action] this one was chained from, see [Result.withAction]
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
