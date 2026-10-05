import { Rejected, Unserved } from "@kiitdev/codes";
import type { Failed } from "@kiitdev/codes";
import type { None, Option } from "./aliases.js";
import { Failure, Success } from "./result.js";
import { asError, createBuilder } from "./builders/builder.js";
import type { ErrorMapper } from "./builders/builder.js";

const mapper: ErrorMapper<None> = {
  fromError: () => undefined,
  fromMessage: () => undefined,
  fromErr: () => undefined,
};

const builder = createBuilder(mapper);

/**
 * Builders for `Option<T>`, a Result where failure means "no value" and carries no error.
 *
 * @example
 * Options.some(42)                       // Success(42)
 * Options.none()                         // Failure, status Rejected.NOT_EXISTS
 * Options.attempt(() => JSON.parse(x))   // None on a throw, status Unserved.UNEXPECTED
 */
export const Options = {
  ...builder,

  /** Runs `f`, with `onError` choosing the failure status for a throw. */
  build<T>(f: () => T, onError: (error: Error) => Failed): Option<T> {
    try {
      return new Success(f());
    } catch (thrown) {
      return new Failure(undefined, onError(asError(thrown)));
    }
  },

  /** Runs `f`. A throw becomes `none` with `Unserved.UNEXPECTED`. */
  attempt<T>(f: () => T): Option<T> {
    return Options.build(f, () => Unserved.UNEXPECTED);
  },

  /** Like `attempt`, for `f` that returns a promise. A rejection becomes `none` too. */
  async attemptAsync<T>(f: () => PromiseLike<T> | T): Promise<Option<T>> {
    try {
      return new Success(await f());
    } catch {
      return new Failure(undefined, Unserved.UNEXPECTED);
    }
  },

  some<T>(value: T): Option<T> {
    return new Success(value);
  },

  /** No value. Status is `Rejected.NOT_EXISTS` unless you pass another Rejected. */
  none(status: Rejected = Rejected.NOT_EXISTS): Option<never> {
    return new Failure(undefined, status);
  },
};
