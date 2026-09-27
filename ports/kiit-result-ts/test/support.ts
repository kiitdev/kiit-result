import { expect } from "vitest";
import { Err, Unserved } from "@kiitdev/codes";
import type { Status } from "@kiitdev/codes";
import { Failure, Success } from "../src/index.js";
import type { Outcome, Result } from "../src/index.js";

// Ported from ResultTestSupport.kt, minus the per-instance status message overrides (not ported).

/** An `Outcome` success, typed as the full `Outcome<T>` union like the Kotlin builders return. */
export function succeed<T>(value: T): Outcome<T> {
  return new Success(value);
}

/** An `Outcome` failure with the default `Unserved.UNEXPECTED` status, typed as `Outcome<T>`. */
export function unserved<T>(message: string): Outcome<T> {
  return new Failure(Err.of(message), Unserved.UNEXPECTED);
}

export function ensureSuccess<T>(result: Result<T, Err>, expectedStatus: Status, expectedValue: T): void {
  expect(result.success).toBe(true);
  expect(result.status.message).toBe(expectedStatus.message);
  expect(result.message).toBe(expectedStatus.message);
  result.onSuccess((value) => expect(value).toEqual(expectedValue));
}

export function ensureFailure<T>(result: Result<T, unknown>, expectedStatus: Status, expectedError?: string): void {
  expect(result.success).toBe(false);
  expect(result.status.message).toBe(expectedStatus.message);
  expect(result.message).toBe(expectedStatus.message);
  result.onFailure((error) => {
    if (typeof error === "string") expect(error).toBe(expectedError);
    else if (typeof error === "object" && error !== null && "kind" in error) {
      expect((error as Err).message).toBe(expectedError);
    } else throw new Error(`Unexpected for: ${String(error)}`);
  });
}
