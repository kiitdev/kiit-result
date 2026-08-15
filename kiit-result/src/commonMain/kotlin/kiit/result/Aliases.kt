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

// Under consideration for the future, not part of the public API yet.
// /**
//  * Alias for Result<T,E> to avoid collision with the Kotlin Result type
//  */
// typealias Expect<T, E> = kiit.result.Result<T, E>

/**
 * Alias for Result<T,E> defaulting the E error type ( [Failure] branch ) to [Unit], for
 * representing a value that may be absent, with a status explaining why. See [Options] for the
 * `some`/`none` entry points and the full design rationale.
 */
typealias Option<T> = Result<T, Unit>

/**
 * Alias for Result<T,E> defaulting the E error type ( [Failure] branch ) to [Throwable], the
 * exception-as-error case, similar in spirit to Scala's `Try`. See [Tries] for the matching
 * builder, and [Result.toTry]/[Result.toOutcome] for converting to and from other aliases.
 */
typealias Try<T> = Result<T, Throwable>

/**
 * Alias for Result<T,E> defaulting the E error type ( [Failure] branch ) to [Err], the most
 * commonly used alias, since [Err] is kiit-codes' own error representation. See [Outcomes] for
 * the matching builder.
 */
typealias Outcome<T> = Result<T, Err>

/**
 * Alias for Result<T,E> defaulting the E error type ( [Failure] branch ) to [Err.ErrorList], for
 * validation, where multiple errors need to be collected and reported together rather than
 * stopping at the first one.
 */
typealias Validated<T> = Result<T, Err.ErrorList>
