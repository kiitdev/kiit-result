import { describe, expect, it } from "vitest";
import {
  Err,
  Excluded,
  Information,
  Invalid,
  Pending,
  Rejected,
  Restricted,
  Succeeded,
  Unserved,
} from "@kiitdev/codes";
import { Outcomes } from "../src/index.js";
import { ensureFailure, ensureSuccess } from "./support.js";

// Ported from ResultBuilderTests.kt, against Outcomes. Kotlin's overloads map to the two call
// forms: `invalid("msg")` / `invalid("msg", { status })` and `invalid({ cause | err | status })`.
// The message-carrying Success cases (`success(42, "life")`) aren't ported, see the spec.

describe("passed builders", () => {
  it("builds successes", () => {
    const status = Succeeded.SUCCESS;
    ensureSuccess(Outcomes.success(), status, null);
    ensureSuccess(Outcomes.success(42), status, 42);
    ensureSuccess(Outcomes.success(42, Succeeded.CREATED), Succeeded.CREATED, 42);
  });

  it("builds pending", () => {
    const status = Pending.ACCEPTED;
    ensureSuccess(Outcomes.pending(), status, null);
    ensureSuccess(Outcomes.pending(42), status, 42);
    ensureSuccess(Outcomes.pending(42, Pending.QUEUED), Pending.QUEUED, 42);
  });

  it("builds excluded as a success, not a failure", () => {
    const status = Excluded.OMITTED;
    ensureSuccess(Outcomes.excluded(), status, null);
    ensureSuccess(Outcomes.excluded(42), status, 42);
    ensureSuccess(Outcomes.excluded(42, Excluded.SKIPPED), Excluded.SKIPPED, 42);
  });

  it("builds information", () => {
    ensureSuccess(Outcomes.information(), Information.NOTICE, null);
    ensureSuccess(Outcomes.information("hi", Information.ADVISORY), Information.ADVISORY, "hi");
  });

  it("keeps an explicitly passed undefined value, unlike no argument", () => {
    expect(Outcomes.success(undefined).getOrNull()).toBeUndefined();
    expect(Outcomes.success().getOrNull()).toBeNull();
  });
});

describe.each([
  ["invalid", Outcomes.invalid, Invalid.INVALID_VALUE, Invalid.BAD_REQUEST],
  ["restricted", Outcomes.restricted, Restricted.DENIED, Restricted.UNAUTHORIZED],
  ["rejected", Outcomes.rejected, Rejected.RULE_VIOLATION, Rejected.CONFLICT],
  ["unserved", Outcomes.unserved, Unserved.UNEXPECTED, Unserved.TIMEOUT],
] as const)("%s builder", (_name, build, defaultStatus, otherStatus) => {
  // The status parameter types differ per group, so widen for the shared calls.
  const call = build as (a?: unknown, b?: unknown) => ReturnType<typeof Outcomes.invalid>;

  it("defaults to the group's default status and message", () => {
    ensureFailure(call(), defaultStatus, defaultStatus.message);
  });

  it("uses a message as the error message", () => {
    ensureFailure(call("x-msg"), defaultStatus, "x-msg");
  });

  it("uses an Error's message", () => {
    ensureFailure(call({ cause: new Error("x-ex") }), defaultStatus, "x-ex");
  });

  it("uses an Err as-is", () => {
    const err = Err.of("x-err");
    const result = call({ err });
    ensureFailure(result, defaultStatus, "x-err");
    expect(result.getErrorOrNull()).toBe(err);
  });

  it("takes a status alone, with the status message as the error", () => {
    ensureFailure(call({ status: otherStatus }), otherStatus, otherStatus.message);
  });

  it("takes a message and a status", () => {
    ensureFailure(call("x-msg", { status: otherStatus }), otherStatus, "x-msg");
  });
});

describe("failure options", () => {
  it("keeps the cause when a message and a cause are both given", () => {
    const cause = new Error("root");
    const result = Outcomes.unserved("wrapper", { cause });
    const err = result.getErrorOrNull();
    expect(err?.message).toBe("wrapper");
    expect(err?.cause?.cause).toBe(cause);
  });

  it("prefers err over cause", () => {
    const err = Err.of("the err");
    const result = Outcomes.invalid({ err, cause: new Error("ignored") });
    expect(result.getErrorOrNull()).toBe(err);
  });

  it("rejects a status from another group at compile time", () => {
    // @ts-expect-error a Rejected status can't go on a restricted failure
    Outcomes.restricted({ status: Rejected.CONFLICT });
    // @ts-expect-error a Failed status can't go on a success
    Outcomes.success(1, Unserved.UNEXPECTED);
  });
});

describe("of / ofIf", () => {
  it("of gives a Success for a value", () => {
    ensureSuccess(Outcomes.of(42), Succeeded.SUCCESS, 42);
  });

  it("of gives an unserved failure for null and undefined", () => {
    ensureFailure(Outcomes.of(null), Unserved.UNEXPECTED, "null");
    ensureFailure(Outcomes.of(undefined), Unserved.UNEXPECTED, "null");
  });

  it("of treats a boolean argument as a value, not a condition", () => {
    ensureSuccess(Outcomes.of(false), Succeeded.SUCCESS, false);
  });

  it("ofIf needs the condition and a value", () => {
    ensureSuccess(Outcomes.ofIf(true, 42), Succeeded.SUCCESS, 42);
    ensureFailure(Outcomes.ofIf(false, 42), Unserved.UNEXPECTED, Unserved.UNEXPECTED.message);
    ensureFailure(Outcomes.ofIf(true, null), Unserved.UNEXPECTED, Unserved.UNEXPECTED.message);
  });

  it("ofIf takes the success and failure statuses", () => {
    ensureSuccess(Outcomes.ofIf(true, 1, { success: Succeeded.CREATED }), Succeeded.CREATED, 1);
    ensureFailure(Outcomes.ofIf(false, 1, { failure: Unserved.TIMEOUT }), Unserved.TIMEOUT, Unserved.TIMEOUT.message);
  });
});
