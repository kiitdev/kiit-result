import { describe, expect, it } from "vitest";
import { Restricted, Unserved } from "@kiitdev/codes";
import { Failure } from "../src/index.js";
import type { Result } from "../src/index.js";

// Ported from ResultConversionTests.kt. Tests toOutcome/toTry's status/error consistency,
// especially the null-error edge case where there's no error value to build a message from.

describe("toOutcome", () => {
  it("with a null error retains the status in both the outer status and the Err message", () => {
    const failure: Result<number, string | null> = new Failure<string | null>(null, Restricted.DENIED);
    const retained = failure.toOutcome(true);
    expect(retained.status).toEqual(Restricted.DENIED);
    expect(retained.getErrorOrNull()?.message).toBe(Restricted.DENIED.message);
  });

  it("with a null error and a discarded status stays consistent", () => {
    const failure: Result<number, string | null> = new Failure<string | null>(null, Restricted.DENIED);
    const discarded = failure.toOutcome(false);
    expect(discarded.status).toEqual(Unserved.UNEXPECTED);
    expect(discarded.getErrorOrNull()?.message).toBe(Unserved.UNEXPECTED.message);
  });

  it("with a non-null error is unaffected", () => {
    const failure: Result<number, string | null> = new Failure("boom", Restricted.DENIED);
    const outcome = failure.toOutcome();
    expect(outcome.status).toEqual(Restricted.DENIED);
    expect(outcome.getErrorOrNull()?.message).toBe("boom");
  });
});

describe("toTry", () => {
  it("keeps an Error as-is, same instance and status", () => {
    const error = new Error("boom");
    const failure = new Failure(error, Restricted.DENIED);
    expect(failure.toTry()).toBe(failure);
  });

  it("builds an Error from a non-Error error, keeping the status", () => {
    const tried = new Failure("boom", Restricted.DENIED).toTry();
    expect(tried.getErrorOrNull()).toBeInstanceOf(Error);
    expect(tried.getErrorOrNull()?.message).toBe("boom");
    expect(tried.status).toEqual(Restricted.DENIED);
  });

  it("builds an Error from a null error using the status message", () => {
    const tried = new Failure(null, Restricted.DENIED).toTry();
    expect(tried.getErrorOrNull()?.message).toBe(Restricted.DENIED.message);
  });
});
