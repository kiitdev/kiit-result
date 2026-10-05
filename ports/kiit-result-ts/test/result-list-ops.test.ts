import { describe, expect, it } from "vitest";
import { Succeeded } from "@kiitdev/codes";
import {
  Action,
  Failure,
  Success,
  allFailure,
  allSuccess,
  anyFailure,
  anySuccess,
  combine,
  partition,
} from "../src/index.js";
import { succeed, unserved } from "./support.js";

// Ported from ResultListOpsTests.kt. The two edge cases at the end of the combine and
// all/any groups (empty lists) are TS-only additions.

describe("combine", () => {
  it("combines all successes, with SUCCESS status and the first item's action", () => {
    const first = new Success("a").withAction(Action("first"));
    const combined = combine([first, succeed("b"), succeed("c")]);
    expect(combined.getOrNull()).toEqual(["a", "b", "c"]);
    expect(combined.status).toEqual(Succeeded.SUCCESS);
    expect(combined.action?.action).toBe("first");
  });

  it("short-circuits on the first failure, returning it as-is", () => {
    const failure = unserved<string>("boom");
    const combined = combine([succeed("a"), failure, succeed("c"), unserved<string>("later")]);
    expect(combined).toBe(failure);
    expect(combined.success).toBe(false);
    expect(combined.getErrorOrNull()?.message).toBe("boom");
  });

  it("gives an empty Success for an empty list", () => {
    const combined = combine([]);
    expect(combined.getOrNull()).toEqual([]);
    expect(combined.action).toBeUndefined();
  });
});

describe("partition", () => {
  it("splits values and errors, in order", () => {
    const [values, errors] = partition([succeed("a"), unserved<string>("boom"), succeed("c")]);
    expect(values).toEqual(["a", "c"]);
    expect(errors.map((e) => e.message)).toEqual(["boom"]);
  });

  it("works with plain (non-Err) errors", () => {
    const [values, errors] = partition([new Success(1), new Failure("x"), new Failure("y")]);
    expect(values).toEqual([1]);
    expect(errors).toEqual(["x", "y"]);
  });
});

describe("allSuccess / allFailure / anySuccess / anyFailure", () => {
  it("checks allSuccess", () => {
    expect(allSuccess([succeed("a"), succeed("b")])).toBe(true);
    expect(allSuccess([succeed("a"), unserved<string>("boom")])).toBe(false);
  });

  it("checks allFailure", () => {
    expect(allFailure([unserved<string>("boom"), unserved<string>("bang")])).toBe(true);
    expect(allFailure([succeed("a"), unserved<string>("boom")])).toBe(false);
  });

  it("checks anySuccess", () => {
    expect(anySuccess([unserved<string>("boom"), succeed("a")])).toBe(true);
    expect(anySuccess([unserved<string>("boom"), unserved<string>("bang")])).toBe(false);
  });

  it("checks anyFailure", () => {
    expect(anyFailure([succeed("a"), unserved<string>("boom")])).toBe(true);
    expect(anyFailure([succeed("a"), succeed("b")])).toBe(false);
  });

  it("treats an empty list as all-true and any-false", () => {
    expect(allSuccess([])).toBe(true);
    expect(allFailure([])).toBe(true);
    expect(anySuccess([])).toBe(false);
    expect(anyFailure([])).toBe(false);
  });
});
