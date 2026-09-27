import { describe, expect, it } from "vitest";
import { Rejected, Succeeded, Unserved } from "@kiitdev/codes";
import { Options } from "../src/index.js";

// Ported from OptionsTests.kt. `none(message)` isn't ported (per-instance status messages).
// Kotlin's `Options.of(f)` is `Options.attempt(f)` here, since `of` already means "value or
// unserved" on every builder.

describe("Options", () => {
  it("builds some", () => {
    const option = Options.some(42);
    expect(option.success).toBe(true);
    expect(option.status).toEqual(Succeeded.SUCCESS);
    expect(option.getOrNull()).toBe(42);
  });

  it("builds none with NOT_EXISTS by default", () => {
    const none = Options.none();
    expect(none.success).toBe(false);
    expect(none.status.message).toBe(Rejected.NOT_EXISTS.message);
    expect(none.getErrorOrNull()).toBeUndefined();
  });

  it("builds none with another Rejected status", () => {
    expect(Options.none(Rejected.CONFLICT).status).toEqual(Rejected.CONFLICT);
  });

  it("carries no error value on any failure builder", () => {
    expect(Options.invalid("bad").getErrorOrNull()).toBeUndefined();
    expect(Options.unserved({ cause: new Error("x") }).getErrorOrNull()).toBeUndefined();
  });

  it("attempt gives Some for a normal return and None for a throw", () => {
    expect(Options.attempt(() => 42).getOrNull()).toBe(42);
    const none = Options.attempt(() => {
      throw new Error("boom");
    });
    expect(none.success).toBe(false);
    expect(none.status).toEqual(Unserved.UNEXPECTED);
  });

  it("build lets the caller pick the status for a throw", () => {
    const none = Options.build(
      () => {
        throw new Error("boom");
      },
      () => Rejected.EXPIRED,
    );
    expect(none.status).toEqual(Rejected.EXPIRED);
  });
});
