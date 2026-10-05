import { Err, Excluded, Information, Invalid, Pending, Rejected, Restricted, Succeeded, Unserved } from "@kiitdev/codes";
import type { Failed, Passed, Status } from "@kiitdev/codes";
import { Failure, Success } from "../result.js";
import type { Result } from "../result.js";

/**
 * Turns the three ways a failure can be described (a message, an Error, an Err) into the error
 * type `E` of a builder. Each kind of Result (Outcome, Try, Option, Validated) supplies its own.
 * `status` is the status the failure is about to be built with.
 */
export interface ErrorMapper<E> {
  fromError(error: Error, status: Status): E;
  fromMessage(message: string | undefined, status: Status): E;
  fromErr(err: Err, status: Status): E;
}

/**
 * Optional pieces of a failure. `status` must be from the builder's own group. When both `err`
 * and `cause` are given, `err` wins. A `cause` given together with a message becomes an Error
 * with that message and the cause attached.
 */
export interface FailureOptions<S extends Failed> {
  readonly status?: S;
  readonly cause?: Error;
  readonly err?: Err;
}

/**
 * Builds a Failure of one group, using the group's default status unless `options.status` says
 * otherwise. Two call forms, message first or options only.
 */
export interface FailureBuilder<S extends Failed, E> {
  (message?: string, options?: FailureOptions<S>): Failure<E>;
  (options: FailureOptions<S>): Failure<E>;
}

/** Builds a Success of one group. With no arguments the value is null. */
export interface PassedBuilder<S extends Passed> {
  (): Success<null>;
  <T>(value: T, status?: S): Success<T>;
}

/**
 * Builders for every group, defaulting each to the group's default status:
 *
 * 1. Passed: success, pending, excluded, information. Excluded is a Success too, an item that
 *    was left out on purpose isn't a failure.
 * 2. Failed: restricted, invalid, rejected, unserved, with the error built by the ErrorMapper.
 */
export interface Builder<E> {
  success: PassedBuilder<Succeeded>;
  pending: PassedBuilder<Pending>;
  excluded: PassedBuilder<Excluded>;
  information: PassedBuilder<Information>;
  restricted: FailureBuilder<Restricted, E>;
  invalid: FailureBuilder<Invalid, E>;
  rejected: FailureBuilder<Rejected, E>;
  unserved: FailureBuilder<Unserved, E>;

  /** A Success for a value, or an `unserved` failure for null or undefined. */
  of<T>(value: T | null | undefined): Result<NonNullable<T>, E>;

  /**
   * A Success if `condition` holds and `value` isn't null or undefined, otherwise an `unserved`
   * failure. Kotlin calls this `of(condition, value)`, it's a separate name here so `of(true)`
   * can't be read as a condition.
   */
  ofIf<T>(
    condition: boolean,
    value: T | null | undefined,
    options?: { readonly success?: Succeeded; readonly failure?: Unserved },
  ): Result<NonNullable<T>, E>;
}

/** Anything thrown, as an Error. Non-Error values are wrapped, with the original as the cause. */
export function asError(thrown: unknown): Error {
  return thrown instanceof Error ? thrown : new Error(String(thrown), { cause: thrown });
}

function passed<S extends Passed>(base: S): PassedBuilder<S> {
  function build<T>(...args: [] | [T, S?]): Success<null> | Success<T> {
    return args.length === 0 ? new Success(null, base) : new Success(args[0], args[1] ?? base);
  }
  return build as PassedBuilder<S>;
}

function failed<S extends Failed, E>(mapper: ErrorMapper<E>, base: S): FailureBuilder<S, E> {
  function build(first?: string | FailureOptions<S>, second?: FailureOptions<S>): Failure<E> {
    const message = typeof first === "string" ? first : undefined;
    const options = (typeof first === "string" || first === undefined ? second : first) ?? {};
    const status = options.status ?? base;
    let error: E;
    if (options.err !== undefined) {
      error = mapper.fromErr(options.err, status);
    } else if (options.cause !== undefined) {
      const cause = message === undefined ? options.cause : new Error(message, { cause: options.cause });
      error = mapper.fromError(cause, status);
    } else {
      error = mapper.fromMessage(message, status);
    }
    return new Failure(error, status);
  }
  return build as FailureBuilder<S, E>;
}

/** Builds a Builder for an error type `E`, given how to make an `E` from a message, Error or Err. */
export function createBuilder<E>(mapper: ErrorMapper<E>): Builder<E> {
  const unserved = failed(mapper, Unserved.UNEXPECTED);
  const success = passed(Succeeded.SUCCESS);

  function ofIf<T>(
    condition: boolean,
    value: T | null | undefined,
    options: { readonly success?: Succeeded; readonly failure?: Unserved } = {},
  ): Result<NonNullable<T>, E> {
    if (!condition || value === null || value === undefined) {
      return unserved({ status: options.failure });
    }
    return success(value as NonNullable<T>, options.success);
  }

  return {
    success,
    pending: passed(Pending.ACCEPTED),
    excluded: passed(Excluded.OMITTED),
    information: passed(Information.NOTICE),
    restricted: failed(mapper, Restricted.DENIED),
    invalid: failed(mapper, Invalid.INVALID_VALUE),
    rejected: failed(mapper, Rejected.RULE_VIOLATION),
    unserved,
    of: <T>(value: T | null | undefined) =>
      value === null || value === undefined ? unserved("null") : success(value as NonNullable<T>),
    ofIf,
  };
}
