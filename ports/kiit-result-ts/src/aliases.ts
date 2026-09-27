import type { Err, ErrorList } from "@kiitdev/codes";
import type { Result } from "./result.js";

/** The error type of an `Option`: absence carries no error value. */
export type None = undefined;

/** A `Result` where failure means "no value", with no error detail. */
export type Option<T> = Result<T, None>;

/** A `Result` whose failure is an `Error`. */
export type Try<T> = Result<T, Error>;

/** A `Result` whose failure is a kiit-codes `Err`. The everyday alias. */
export type Outcome<T> = Result<T, Err>;

/** A `Result` whose failure is a list of validation errors. */
export type Validated<T> = Result<T, ErrorList>;
