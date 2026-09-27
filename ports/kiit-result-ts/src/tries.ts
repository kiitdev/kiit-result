import { InvalidError, RejectedError, RestrictedError, UnservedError } from "@kiitdev/codes";
import type { Invalid, Rejected, Restricted, Unserved } from "@kiitdev/codes";
import type { Try } from "./aliases.js";
import { Success } from "./result.js";
import { asError, createBuilder } from "./builders/builder.js";
import type { ErrorMapper } from "./builders/builder.js";

const mapper: ErrorMapper<Error> = {
  fromError: (error) => error,
  fromMessage: (message, status) => new Error(message ?? status.message),
  fromErr: (err) => new Error(err.message, { cause: err.cause }),
};

const builder = createBuilder(mapper);

/**
 * Builders for `Try<T>`, a Result whose failure is an Error.
 *
 * @example
 * Tries.attempt(() => JSON.parse(text))  // Failure holds the thrown Error
 */
export const Tries = {
  ...builder,

  /**
   * Runs `f`. A throw becomes a Failure holding the Error. A StatusError from kiit-codes keeps
   * its own status, so a RestrictedError gives a `restricted` failure with that status. Anything
   * else is `Unserved.UNEXPECTED`.
   */
  attempt<T>(f: () => T): Try<T> {
    try {
      return new Success(f());
    } catch (thrown) {
      if (thrown instanceof RestrictedError) return builder.restricted({ cause: thrown, status: thrown.status as Restricted });
      if (thrown instanceof InvalidError) return builder.invalid({ cause: thrown, status: thrown.status as Invalid });
      if (thrown instanceof RejectedError) return builder.rejected({ cause: thrown, status: thrown.status as Rejected });
      if (thrown instanceof UnservedError) return builder.unserved({ cause: thrown, status: thrown.status as Unserved });
      return builder.unserved({ cause: asError(thrown) });
    }
  },
};
