import { Succeeded } from "@kiitdev/codes";
import { Success } from "./result.js";
import type { Result } from "./result.js";

/**
 * Turns a list of Results into one Result of a list. Every item a Success gives a Success with
 * all the values. The first Failure is returned as-is, status and action unchanged.
 *
 * One Success can't carry N different statuses, so the combined one gets Succeeded.SUCCESS and
 * the first item's action. Use withAction if you want a different one.
 *
 * @example
 * combine([new Success(1), new Success(2)])                 // Success([1, 2])
 * combine([new Success(1), new Failure("boom"), new Success(3)]) // Failure("boom")
 */
export function combine<T, E>(results: readonly Result<T, E>[]): Result<T[], E> {
  const values: T[] = [];
  for (const result of results) {
    if (!result.success) return result;
    values.push(result.value);
  }
  return new Success(values, Succeeded.SUCCESS, results[0]?.action);
}

/**
 * Splits a list into its Success values and Failure errors, in order. Unlike combine, this
 * doesn't build a new Result, so there's no status to decide.
 *
 * @example
 * partition([new Success(1), new Failure("boom"), new Success(3)]) // [[1, 3], ["boom"]]
 */
export function partition<T, E>(results: readonly Result<T, E>[]): readonly [T[], E[]] {
  const values: T[] = [];
  const errors: E[] = [];
  for (const result of results) {
    if (result.success) values.push(result.value);
    else errors.push(result.error);
  }
  return [values, errors];
}

/** True if every item is a Success. True for an empty list. */
export function allSuccess<T, E>(results: readonly Result<T, E>[]): boolean {
  return results.every((r) => r.success);
}

/** True if every item is a Failure. True for an empty list. */
export function allFailure<T, E>(results: readonly Result<T, E>[]): boolean {
  return results.every((r) => !r.success);
}

/** True if at least one item is a Success. */
export function anySuccess<T, E>(results: readonly Result<T, E>[]): boolean {
  return results.some((r) => r.success);
}

/** True if at least one item is a Failure. */
export function anyFailure<T, E>(results: readonly Result<T, E>[]): boolean {
  return results.some((r) => !r.success);
}
