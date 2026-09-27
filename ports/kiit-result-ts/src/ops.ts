import type { Result } from "./result.js";

/**
 * The value, or throws the failure's error as-is, same instance and stack trace. Only takes a
 * Result whose error is an Error, like a Try.
 *
 * @example
 * getOrRethrow(new Success(42))                 // 42
 * getOrRethrow(new Failure(new Error("boom"))) // throws that same Error
 */
export function getOrRethrow<T>(result: Result<T, Error>): T {
  if (result.success) return result.value;
  throw result.error;
}

/**
 * Flattens a nested Result into one, same as Rust's flatten.
 *
 * An outer Failure comes back unchanged, status and action included. (The Kotlin version builds
 * a new Failure with the default status here, which drops them.)
 *
 * @example
 * flatten(new Success(new Success("guest"))) // Success("guest")
 */
export function flatten<T, E>(result: Result<Result<T, E>, E>): Result<T, E> {
  return result.success ? result.value : result;
}

