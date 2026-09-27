import { describe, expect, it } from "vitest";
import {
  InvalidError,
  Invalid,
  RejectedError,
  Rejected,
  RestrictedError,
  Restricted,
  Succeeded,
  UnservedError,
  Unserved,
} from "@kiitdev/codes";
import { Tries } from "../src/index.js";

// TS-only tests for Tries.attempt (Kotlin covers it only through ActionTests).

describe("Tries.attempt", () => {
  it("returns a Success for a normal return", () => {
    const result = Tries.attempt(() => 42);
    expect(result.getOrNull()).toBe(42);
    expect(result.status).toEqual(Succeeded.SUCCESS);
  });

  it("keeps the thrown Error as the failure, same instance", () => {
    const error = new Error("boom");
    const result = Tries.attempt(() => {
      throw error;
    });
    expect(result.success).toBe(false);
    expect(result.getErrorOrNull()).toBe(error);
    expect(result.status).toEqual(Unserved.UNEXPECTED);
  });

  it("wraps a thrown non-Error", () => {
    const result = Tries.attempt(() => {
      throw "just a string";
    });
    expect(result.getErrorOrNull()).toBeInstanceOf(Error);
    expect(result.getErrorOrNull()?.message).toBe("just a string");
  });

  it.each([
    ["RestrictedError", new RestrictedError(Restricted.UNAUTHORIZED), Restricted.UNAUTHORIZED],
    ["InvalidError", new InvalidError(Invalid.BAD_REQUEST), Invalid.BAD_REQUEST],
    ["RejectedError", new RejectedError(Rejected.CONFLICT), Rejected.CONFLICT],
    ["UnservedError", new UnservedError(Unserved.TIMEOUT), Unserved.TIMEOUT],
  ])("keeps the status of a %s", (_name, thrown, status) => {
    const result = Tries.attempt(() => {
      throw thrown;
    });
    expect(result.status).toEqual(status);
    expect(result.getErrorOrNull()).toBe(thrown);
  });
});

describe("Tries builders", () => {
  it("build an Error from a message or an Err", () => {
    expect(Tries.invalid("bad").getErrorOrNull()?.message).toBe("bad");
    expect(Tries.invalid().getErrorOrNull()?.message).toBe(Invalid.INVALID_VALUE.message);
  });
});
