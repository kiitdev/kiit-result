import { Err } from "@kiitdev/codes";
import { Action } from "./action.js";
import type { Outcome } from "./aliases.js";
import { Failure, Success } from "./result.js";
import { asError, createBuilder } from "./builders/builder.js";
import type { ErrorMapper } from "./builders/builder.js";

const mapper: ErrorMapper<Err> = {
  fromError: (error) => Err.ex(error),
  fromMessage: (message, status) => Err.of(message ?? status.message),
  fromErr: (err) => err,
};

/** Runs `f`, turning a thrown value into a Failure with the error built by `onError`. */
function build<T>(f: () => T, onError: (error: Error) => Err): Outcome<T> {
  try {
    return new Success(f());
  } catch (thrown) {
    return new Failure(onError(asError(thrown)));
  }
}

function attempt<T>(f: () => T): Outcome<T>;
function attempt<T>(action: string | Action, f: () => T): Outcome<T>;
function attempt<T>(first: (() => T) | string | Action, second?: () => T): Outcome<T> {
  if (typeof first === "function") return build(first, Err.ex);
  const action = typeof first === "string" ? Action(first) : first;
  return build(second as () => T, Err.ex).withAction(action);
}

/**
 * Builders for `Outcome<T>`, a Result whose failure is a kiit-codes Err. Everything from Builder
 * (success, invalid, unserved and so on) plus the entry points below.
 *
 * @example
 * Outcomes.attempt(() => JSON.parse(text))                    // Failure holds an Err on a throw
 * Outcomes.attempt("parseConfig", () => JSON.parse(text))     // same, tagged with an Action
 * Outcomes.invalid("email is required")                       // Failure, status INVALID_VALUE
 * Outcomes.restricted({ status: Restricted.UNAUTHORIZED })    // Failure, status UNAUTHORIZED
 */
export const Outcomes = {
  ...createBuilder(mapper),

  /** Runs `f`. A throw becomes a Failure with an Err and `Unserved.UNEXPECTED`. */
  attempt,

  /** Runs `f`, with `onError` choosing the Err for a throw. */
  build,

  /**
   * Tags the Outcome that `op` returns with an action, without catching anything. With `chain`
   * (default) the action links to one already on the outcome, see Result.withAction.
   */
  withAction<T>(action: string | Action, op: () => Outcome<T>, chain: boolean = true): Outcome<T> {
    return op().withAction(typeof action === "string" ? Action(action) : action, chain);
  },
};
