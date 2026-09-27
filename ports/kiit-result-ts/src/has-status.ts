import type { Failed, HasStatus, Passed, Status } from "@kiitdev/codes";
import { Failure, Success } from "./result.js";
import type { Result } from "./result.js";

/**
 * A Success for a domain value that carries its own Passed status.
 *
 * @example
 * success(new UserCreated("a@b.com"))  // Success, status is the value's own
 */
export function success<T extends HasStatus<Passed>>(value: T): Success<T> {
  return new Success(value, value.status);
}

/** A Failure for a domain error that carries its own Failed status. */
export function failure<E extends HasStatus<Failed>>(error: E): Failure<E> {
  return new Failure(error, error.status);
}

/**
 * A Success or Failure for one domain type that covers both branches, picked from whether the
 * value's own status is a Passed or a Failed.
 */
export function build<T extends HasStatus<Status>>(value: T): Result<T, T> {
  const status = value.status;
  return status.success ? new Success(value, status) : new Failure(value, status);
}
