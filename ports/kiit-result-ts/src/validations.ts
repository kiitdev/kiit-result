import { Err, ErrorList, Invalid } from "@kiitdev/codes";
import type { Failed } from "@kiitdev/codes";
import type { Validated } from "./aliases.js";
import { Failure, Success } from "./result.js";
import { createBuilder } from "./builders/builder.js";
import type { ErrorMapper } from "./builders/builder.js";

const mapper: ErrorMapper<ErrorList> = {
  fromError: (error, status) => ErrorList([Err.ex(error)], error.message || status.message),
  fromMessage: (message, status) => ErrorList([Err.of(message ?? status.message)], message ?? status.message),
  fromErr: (err) => (err.kind === "ErrorList" ? err : ErrorList([err], err.message)),
};

export interface ValidationOptions {
  /** Message for the ErrorList. Defaults to "Validation failed with N error(s)". */
  readonly message?: string;
  readonly status?: Invalid;
}

/**
 * Builders for `Validated<T>`, a Result whose failure is an ErrorList, so every problem can be
 * reported at once.
 *
 * @example
 * Validations.of(user, [Err.on("name", "", "Name is required")])  // Failure with 1 error
 * Validations.of(user, [])                                        // Success(user)
 */
export const Validations = {
  ...createBuilder(mapper),

  /** A Success holding `value` if `errors` is empty, otherwise a Failure holding all of them. */
  of<T>(value: T, errors: readonly Err[], options: ValidationOptions = {}): Validated<T> {
    if (errors.length === 0) return new Success(value);
    const message = options.message ?? `Validation failed with ${errors.length} error(s)`;
    const status: Failed = options.status ?? Invalid.INVALID_VALUE;
    return new Failure(ErrorList(errors, message), status);
  },
};
