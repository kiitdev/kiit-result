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
import kiit.codes.Invalid
import kiit.codes.Status
import kiit.result.builders.Builder
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Builds [Result] with [Failure] error type of [Err.ErrorList]
 */
interface ValidationBuilder : Builder<Err.ErrorList> {
    override fun errorFromEx(ex: Throwable, defaultStatus: Status): Err.ErrorList =
        Err.ErrorList(listOf(Err.ex(ex)), ex.message ?: defaultStatus.message)

    override fun errorFromStr(message: String?, defaultStatus: Status): Err.ErrorList =
        Err.ErrorList(listOf(Err.of(message ?: defaultStatus.message)), message ?: defaultStatus.message)

    override fun errorFromErr(err: Err, defaultStatus: Status): Err.ErrorList =
        if (err is Err.ErrorList) err else Err.ErrorList(listOf(err), err.message)
}

/**
 * Builds [Result] with [Failure] error type of [Err.ErrorList]. [of] is the main entry point:
 * given a value and the errors found while validating it, it builds a [Success] if there are
 * none, or a single [Failure] carrying every error together, instead of stopping at the first.
 */
object Validations : ValidationBuilder {
    /**
     * Build a [Validated] from a [value] and the [errors] found while validating it.
     *
     * # Example
     * ```
     * val errors = mutableListOf<Err>()
     * if (form.name.isBlank()) errors.add(Err.on("name", form.name, "Name is required"))
     * Validations.of(form, errors)
     * ```
     */
    @JvmStatic
    @JvmOverloads
    fun <T> of(
        value: T,
        errors: List<Err>,
        message: String? = null,
        status: Failed.Invalid = Invalid.INVALID_VALUE,
    ): Validated<T> =
        if (errors.isEmpty()) {
            Success(value)
        } else {
            Failure(Err.ErrorList(errors, message ?: "Validation failed with ${errors.size} error(s)"), status)
        }
}
