import { describe, expect, it } from "vitest";
import { Err, Excluded, Invalid, Pending, Rejected, Restricted, Succeeded, Unserved } from "@kiitdev/codes";
import { Failure, Outcomes, Success } from "../src/index.js";
import { ensureFailure, ensureSuccess } from "./support.js";

// Ported from ResultStatusTests.kt. Not ported: the cases that pass a per-instance message to
// Success/Failure (`Success(42, "created")`), since the TS port doesn't have per-instance status
// messages.

describe("Status taxonomy as seen through Result", () => {
  it("has the success flag per group", () => {
    expect(Succeeded.SUCCESS.success).toBe(true);
    expect(Pending.ACCEPTED.success).toBe(true);
    // Excluded is a Passed group, an excluded item is still a success.
    expect(Excluded.SKIPPED.success).toBe(true);
    expect(Invalid.BAD_REQUEST.success).toBe(false);
    expect(Restricted.UNAUTHENTICATED.success).toBe(false);
    expect(Rejected.CONFLICT.success).toBe(false);
    expect(Unserved.UNEXPECTED.success).toBe(false);
  });
});

describe("Success", () => {
  it("defaults to Succeeded.SUCCESS", () => {
    ensureSuccess(new Success(42), Succeeded.SUCCESS, 42);
    ensureSuccess(Success.of(42), Succeeded.SUCCESS, 42);
    ensureSuccess(Outcomes.pending(42), Pending.ACCEPTED, 42);
  });

  it("keeps an explicit status", () => {
    ensureSuccess(new Success(42, Pending.ACCEPTED), Pending.ACCEPTED, 42);
  });
});

describe("Failure", () => {
  it("defaults to Unserved.UNEXPECTED", () => {
    ensureFailure(new Failure("invalid email"), Unserved.UNEXPECTED, "invalid email");
    ensureFailure(Failure.of("invalid email"), Unserved.UNEXPECTED, "invalid email");
  });

  it("holds an Err as its error", () => {
    ensureFailure(new Failure(Err.of("invalid email")), Unserved.UNEXPECTED, "invalid email");
  });

  it("keeps an explicit status", () => {
    ensureFailure(new Failure(Err.of("invalid email"), Invalid.BAD_REQUEST), Invalid.BAD_REQUEST, "invalid email");
    ensureFailure(
      Outcomes.invalid({ err: Err.of("invalid email"), status: Invalid.BAD_REQUEST }),
      Invalid.BAD_REQUEST,
      "invalid email",
    );
  });
});
