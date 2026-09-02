/** url: www.kiit.dev */

package kiit.result

import kiit.codes.Failed
import kiit.codes.HasStatus
import kiit.codes.Passed
import kiit.codes.Status

/**
 * Builds a [Success] from a [HasStatus]-carrying domain value, using its own [HasStatus.status]
 * instead of [Success]'s own default. This wiring isn't automatic: `value: T` and `status: Passed`
 * are independent [Success] constructor parameters, see [Result]'s Relationship docs.
 */
fun <T : HasStatus<Passed>, E> success(value: T): Result<T, E> = Success(value, value.status)

/**
 * Builds a [Failure] from a [HasStatus]-carrying domain error, using its own [HasStatus.status]
 * instead of [Failure]'s own default. This wiring isn't automatic: `error: E` and `status: Failed`
 * are independent [Failure] constructor parameters, see [Result]'s Relationship docs.
 */
fun <T, E : HasStatus<Failed>> failure(error: E): Result<T, E> = Failure(error, error.status)

/**
 * Builds a [Result] from a single [HasStatus]-carrying domain type that can represent either
 * branch, inspecting its own [HasStatus.status] at runtime to decide [Success] or [Failure].
 * Useful when one sealed hierarchy enumerates every possible outcome of an operation, success and
 * failure alike, instead of splitting them into two separate types (see [success]/[failure]).
 */
fun <T : HasStatus<Status>> build(value: T): Result<T, T> =
    when (val status = value.status) {
        is Passed -> Success(value, status)
        is Failed -> Failure(value, status)
    }
