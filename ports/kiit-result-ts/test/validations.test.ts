import { describe, expect, it } from "vitest";
import { Err, ErrorList, Invalid } from "@kiitdev/codes";
import { Validations } from "../src/index.js";

// Ported from ValidationsTests.kt. `Validations.of(value, errors, message, status)` takes the
// last two in an options object.

describe("Validations.of", () => {
  it("builds a success when there are no errors", () => {
    const result = Validations.of("alice", []);
    expect(result.success).toBe(true);
    expect(result.getOrNull()).toBe("alice");
  });

  it("builds a failure with the default message and status", () => {
    const errors = [Err.on("name", "", "Name is required")];
    const result = Validations.of("alice", errors);

    expect(result.success).toBe(false);
    expect(result.status).toEqual(Invalid.INVALID_VALUE);
    const list = result.getErrorOrNull();
    expect(list?.errors).toEqual(errors);
    expect(list?.message).toBe("Validation failed with 1 error(s)");
  });

  it("collects multiple errors", () => {
    const errors = [
      Err.on("name", "", "Name is required"),
      Err.on("email", "bad", "Email must contain @"),
      Err.on("phone", "123", "Phone must be 10 digits"),
    ];
    const list = Validations.of("alice", errors).getErrorOrNull();
    expect(list?.errors).toHaveLength(3);
    expect(list?.errors).toEqual(errors);
    expect(list?.message).toBe("Validation failed with 3 error(s)");
  });

  it("can override the message and status", () => {
    const result = Validations.of("alice", [Err.on("name", "", "Name is required")], {
      message: "custom message",
      status: Invalid.BAD_REQUEST,
    });
    expect(result.status).toEqual(Invalid.BAD_REQUEST);
    expect(result.getErrorOrNull()?.message).toBe("custom message");
  });
});

describe("inherited builders", () => {
  it("wrap a single error in a list", () => {
    const result = Validations.invalid("bad input");
    expect(result.success).toBe(false);
    const list = result.getErrorOrNull();
    expect(list?.errors).toHaveLength(1);
    expect(list?.errors[0]?.message).toBe("bad input");
  });

  it("wrap an Error in a list", () => {
    const list = Validations.invalid({ cause: new Error("bad ex") }).getErrorOrNull();
    expect(list?.errors[0]?.message).toBe("bad ex");
    expect(list?.message).toBe("bad ex");
  });

  it("don't double-wrap an existing ErrorList", () => {
    const original = ErrorList([Err.of("first"), Err.of("second")], "multiple");
    const result = Validations.invalid({ err: original });
    expect(result.getErrorOrNull()).toBe(original);
  });

  it("wrap a single non-list Err in a list", () => {
    const err = Err.of("one");
    const list = Validations.invalid({ err }).getErrorOrNull();
    expect(list?.errors).toEqual([err]);
  });
});
