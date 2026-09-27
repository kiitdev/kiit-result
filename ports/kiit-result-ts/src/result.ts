/**
 * Models successes and failures, each carrying a kiit-codes Status. Similar to Result in Rust
 * and Swift, or Try in Scala.
 *
 * 1. Flexible error: E can be anything, an Error, an Err, a string.
 * 2. Status on both branches: Success.status is a Passed and Failure.status is a Failed, so
 *    narrowing on `success` narrows the group too.
 * 3. Shaped like the Kotlin version: Success<T> and Failure<E> each take one type parameter
 *    (Kotlin's Result<T, Nothing> and Result<Nothing, E>, with never for Nothing), and
 *    Result<T, E> is their union.
 *
 * There's no `then` method on purpose. An object with `then` is a thenable, so `await`,
 * Promise.resolve and returning it from an async function would call it as a promise callback
 * and break. Use flatMap, or its alias andThen.
 */

import { Err, Succeeded, Unserved, toError } from "@kiitdev/codes";
import type { Failed, Passed, Status } from "@kiitdev/codes";
import { Action } from "./action.js";
import type { Outcome, Try } from "./aliases.js";

/** Either branch of a result. Narrow with `if (r.success)` or `switch (r.success)`. */
export type Result<T, E> = Success<T> | Failure<E>;

/**
 * The shared shape of Success and Failure. Use Result in signatures, not this.
 *
 * Where a method lives:
 * 1. Behavior that differs per branch (map, onSuccess, getOrNull) is implemented in Success and
 *    Failure, so each concrete type keeps its precise return type, like Success<T2> from map.
 * 2. Behavior built only from other methods, with no branch check (message, andThen), is
 *    implemented once here.
 *
 * If a new method here needs an `if (success)` check plus a cast, it belongs in the two
 * subclasses instead.
 */
export abstract class ResultBase<out T, out E> {
  abstract readonly success: boolean;
  abstract readonly status: Status;
  abstract readonly action?: Action;

  /** The status' message. */
  get message(): string {
    return this.status.message;
  }

  /** Applies `f` to the value if this is a `Success`. Keeps status and action. */
  abstract map<T2>(f: (value: T) => T2): Result<T2, E>;

  /** Applies `f` to the error if this is a `Failure`. Keeps status and action. */
  abstract mapError<E2>(f: (error: E) => E2): Result<T, E2>;

  /** Applies `onSuccess` or `onFailure`, whichever branch this is, and returns its result. */
  abstract fold<R>(onSuccess: (value: T) => R, onFailure: (error: E) => R): R;

  /** True if this is a `Success` whose value satisfies `f`. */
  abstract exists(f: (value: T) => boolean): boolean;

  /** True if this is a `Failure` whose error satisfies `f`. */
  abstract existsError(f: (error: E) => boolean): boolean;

  /** The value if this is a `Success`, otherwise `null`. */
  abstract getOrNull(): T | null;

  /** The error if this is a `Failure`, otherwise `null`. */
  abstract getErrorOrNull(): E | null;

  /** The value, or `fallback` if this is a `Failure`. */
  abstract getOr<U>(fallback: U): T | U;

  /** The value, or the result of `f(error)` if this is a `Failure`. */
  abstract getOrElse<U>(f: (error: E) => U): T | U;

  /**
   * The value, or throws if this is a `Failure`. An `Error` error is rethrown as-is, an `Err`
   * becomes the matching `StatusError`, anything else is wrapped in an `Error`. With `message`,
   * throws `Error("<message>: <failure message>")` with the failure as its `cause`.
   */
  abstract getOrThrow(message?: () => unknown): T;

  /** The error, or throws if this is a `Success`. */
  abstract getErrorOrThrow(message?: () => unknown): E;

  /** Runs `f` with the value if this is a `Success`. Returns this. */
  abstract onSuccess(f: (value: T) => void): this;

  /** Runs `f` with the error if this is a `Failure`. Returns this. */
  abstract onFailure(f: (error: E) => void): this;

  /** Chains an operation that itself returns a `Result`. A `Failure` short-circuits. */
  abstract flatMap<T2, E2>(f: (value: T) => Result<T2, E2>): Result<T2, E | E2>;

  /** Alias of `flatMap`, named as in Rust. */
  andThen<T2, E2>(f: (value: T) => Result<T2, E2>): Result<T2, E | E2> {
    return this.flatMap(f);
  }

  /** This if it's a `Success`, otherwise `other`. */
  abstract or<T2, E2>(other: Result<T2, E2>): Result<T | T2, E2>;

  /** `other` if this is a `Success`, otherwise this. */
  abstract and<T2, E2>(other: Result<T2, E2>): Result<T2, E | E2>;

  /** This if it's a `Success`, otherwise the result of `f(error)`. */
  abstract orElse<T2, E2>(f: (error: E) => Result<T2, E2>): Result<T | T2, E2>;

  /** Turns a `Failure` into a `Success` with `Succeeded.SUCCESS`, keeping the action. */
  abstract recover<T2>(f: (error: E) => T2): Success<T | T2>;

  /** Applies one of two functions that each return a `Result`, depending on the branch. */
  abstract transform<T2, E2>(
    onSuccess: (value: T) => Result<T2, E2>,
    onFailure: (error: E) => Result<T2, E2>,
  ): Result<T2, E2>;

  /** Replaces the status: `passed` if this is a `Success`, `failed` if a `Failure`. */
  abstract withStatus(passed: Passed, failed: Failed): Result<T, E>;

  /**
   * Attaches `action`. With `chain` (default), and when `action` has no `previous` of its own,
   * links it to the action already on this result, preserving history across nested operations.
   */
  abstract withAction(action: Action, chain?: boolean): Result<T, E>;

  /**
   * This as an `Outcome`, converting the error to an `Err`. Without `retainStatus`, the status
   * becomes `Unserved.UNEXPECTED`.
   */
  abstract toOutcome(retainStatus?: boolean): Outcome<T>;

  /** This as a `Try`, converting the error to an `Error`. */
  abstract toTry(): Try<T>;
}

/** Success branch. `status` defaults to `Succeeded.SUCCESS`. */
export class Success<out T> extends ResultBase<T, never> {
  readonly success = true as const;

  constructor(
    readonly value: T,
    readonly status: Passed = Succeeded.SUCCESS,
    readonly action?: Action,
  ) {
    super();
  }

  static of<T>(value: T): Success<T> {
    return new Success(value);
  }

  map<T2>(f: (value: T) => T2): Success<T2> {
    return new Success(f(this.value), this.status, this.action);
  }

  mapError<E2>(_f: (error: never) => E2): Success<T> {
    return this;
  }

  fold<R>(onSuccess: (value: T) => R, _onFailure: (error: never) => R): R {
    return onSuccess(this.value);
  }

  exists(f: (value: T) => boolean): boolean {
    return f(this.value);
  }

  existsError(_f: (error: never) => boolean): boolean {
    return false;
  }

  getOrNull(): T {
    return this.value;
  }

  getErrorOrNull(): null {
    return null;
  }

  getOr<U>(_fallback: U): T {
    return this.value;
  }

  getOrElse<U>(_f: (error: never) => U): T {
    return this.value;
  }

  getOrThrow(_message?: () => unknown): T {
    return this.value;
  }

  getErrorOrThrow(message?: () => unknown): never {
    const prefix = message ? String(message()) : "getErrorOrThrow() called on a Success value";
    throw new Error(`${prefix}: ${String(this.value)}`);
  }

  onSuccess(f: (value: T) => void): this {
    f(this.value);
    return this;
  }

  onFailure(_f: (error: never) => void): this {
    return this;
  }

  flatMap<T2, E2>(f: (value: T) => Result<T2, E2>): Result<T2, E2> {
    return f(this.value);
  }

  or<T2, E2>(_other: Result<T2, E2>): Success<T> {
    return this;
  }

  and<T2, E2>(other: Result<T2, E2>): Result<T2, E2> {
    return other;
  }

  orElse<T2, E2>(_f: (error: never) => Result<T2, E2>): Success<T> {
    return this;
  }

  recover<T2>(_f: (error: never) => T2): Success<T> {
    return this;
  }

  transform<T2, E2>(
    onSuccess: (value: T) => Result<T2, E2>,
    _onFailure: (error: never) => Result<T2, E2>,
  ): Result<T2, E2> {
    return onSuccess(this.value);
  }

  withStatus(passed: Passed, _failed: Failed): Success<T> {
    return new Success(this.value, passed, this.action);
  }

  withAction(action: Action, chain: boolean = true): Success<T> {
    return new Success(this.value, this.status, chainAction(action, this.action, chain));
  }

  toOutcome(_retainStatus?: boolean): Success<T> {
    return this;
  }

  toTry(): Success<T> {
    return this;
  }
}

/** Failure branch. `status` defaults to `Unserved.UNEXPECTED`. */
export class Failure<out E> extends ResultBase<never, E> {
  readonly success = false as const;

  constructor(
    readonly error: E,
    readonly status: Failed = Unserved.UNEXPECTED,
    readonly action?: Action,
  ) {
    super();
  }

  static of<E>(error: E): Failure<E> {
    return new Failure(error);
  }

  map<T2>(_f: (value: never) => T2): Failure<E> {
    return this;
  }

  mapError<E2>(f: (error: E) => E2): Failure<E2> {
    return new Failure(f(this.error), this.status, this.action);
  }

  fold<R>(_onSuccess: (value: never) => R, onFailure: (error: E) => R): R {
    return onFailure(this.error);
  }

  exists(_f: (value: never) => boolean): boolean {
    return false;
  }

  existsError(f: (error: E) => boolean): boolean {
    return f(this.error);
  }

  getOrNull(): null {
    return null;
  }

  getErrorOrNull(): E {
    return this.error;
  }

  getOr<U>(fallback: U): U {
    return fallback;
  }

  getOrElse<U>(f: (error: E) => U): U {
    return f(this.error);
  }

  getOrThrow(message?: () => unknown): never {
    const cause = this.toThrowable();
    if (message) {
      throw new Error(`${String(message())}: ${cause.message}`, { cause });
    }
    throw cause;
  }

  getErrorOrThrow(_message?: () => unknown): E {
    return this.error;
  }

  onSuccess(_f: (value: never) => void): this {
    return this;
  }

  onFailure(f: (error: E) => void): this {
    f(this.error);
    return this;
  }

  flatMap<T2, E2>(_f: (value: never) => Result<T2, E2>): Failure<E> {
    return this;
  }

  or<T2, E2>(other: Result<T2, E2>): Result<T2, E2> {
    return other;
  }

  and<T2, E2>(_other: Result<T2, E2>): Failure<E> {
    return this;
  }

  orElse<T2, E2>(f: (error: E) => Result<T2, E2>): Result<T2, E2> {
    return f(this.error);
  }

  recover<T2>(f: (error: E) => T2): Success<T2> {
    return new Success(f(this.error), Succeeded.SUCCESS, this.action);
  }

  transform<T2, E2>(_onSuccess: (value: never) => Result<T2, E2>, onFailure: (error: E) => Result<T2, E2>): Result<T2, E2> {
    return onFailure(this.error);
  }

  withStatus(_passed: Passed, failed: Failed): Failure<E> {
    return new Failure(this.error, failed, this.action);
  }

  withAction(action: Action, chain: boolean = true): Failure<E> {
    return new Failure(this.error, this.status, chainAction(action, this.action, chain));
  }

  toOutcome(retainStatus: boolean = true): Failure<Err> {
    const status = retainStatus ? this.status : Unserved.UNEXPECTED;
    const err = this.error === null || this.error === undefined ? Err.ofStatus(status) : Err.build(this.error);
    return new Failure(err, status, this.action);
  }

  toTry(): Failure<Error> {
    // An Error error is kept as-is, status and all, same as the Kotlin version.
    if (this.error instanceof Error) return this as Failure<Error>;
    return new Failure(this.toThrowable(), this.status, this.action);
  }

  /** The error as an `Error`, so nothing gets dropped. Internal, don't call it directly. */
  toThrowable(): Error {
    const error: unknown = this.error;
    if (error instanceof Error) return error;
    if (error === null || error === undefined) return new Error(this.status.message);
    if (typeof error === "object" && "kind" in error && "message" in error) {
      return toError(this.status, [error as Err]);
    }
    return new Error(String(error));
  }
}

function chainAction(action: Action, existing: Action | undefined, chain: boolean): Action {
  return chain ? { ...action, previous: action.previous ?? existing } : action;
}
