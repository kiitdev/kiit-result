import { describe, expect, expectTypeOf, it } from "vitest";
import { Succeeded, Unserved, assertNever } from "@kiitdev/codes";
import type { Failed, Passed } from "@kiitdev/codes";
import { Failure, Success } from "../src/index.js";
import type { Result } from "../src/index.js";

// TS-only tests, no Kotlin equivalent: narrowing, exhaustiveness, variance and thenable safety.

describe("narrowing", () => {
  it("narrows on `success`, including the status group", () => {
    function check(r: Result<number, string>): void {
      if (r.success) {
        expectTypeOf(r.value).toEqualTypeOf<number>();
        expectTypeOf(r.status).toEqualTypeOf<Passed>();
      } else {
        expectTypeOf(r.error).toEqualTypeOf<string>();
        expectTypeOf(r.status).toEqualTypeOf<Failed>();
      }
    }
    check(new Success(1));
    check(new Failure("x"));
  });

  it("is exhaustive with assertNever", () => {
    function describeResult(r: Result<number, string>): string {
      switch (r.success) {
        case true:
          return `ok ${r.value}`;
        case false:
          return `err ${r.error}`;
        default:
          return assertNever(r);
      }
    }
    expect(describeResult(new Success(1))).toBe("ok 1");
    expect(describeResult(new Failure("no"))).toBe("err no");
  });
});

describe("variance", () => {
  it("assigns Success and Failure to a wider Result", () => {
    const ok: Result<number, string> = new Success(1);
    const bad: Result<number, string> = new Failure("x");
    expect([ok.success, bad.success]).toEqual([true, false]);
  });

  it("widens the error type through flatMap", () => {
    const r = new Success(1).flatMap((n) => (n > 0 ? new Success(String(n)) : new Failure(new Error("neg"))));
    expectTypeOf(r).toEqualTypeOf<Result<string, Error>>();
  });

  it("rejects mismatched status groups", () => {
    // @ts-expect-error a Failed status can't go on a Success
    new Success(1, Unserved.UNEXPECTED);
    // @ts-expect-error a Passed status can't go on a Failure
    new Failure("x", Succeeded.SUCCESS);
  });
});

describe("thenable safety", () => {
  it("await returns the same Result, it is not a thenable", async () => {
    const success = new Success(42);
    const failure = new Failure("boom");
    expect(await success).toBe(success);
    expect(await Promise.resolve(failure)).toBe(failure);
    async function load(): Promise<Result<number, string>> {
      return success;
    }
    expect(await load()).toBe(success);
    expect("then" in success).toBe(false);
  });
});

describe("immutability", () => {
  it("methods return new results and leave the original alone", () => {
    const original = new Success(1);
    const mapped = original.map((n) => n + 1).withStatus(Succeeded.CREATED, Unserved.UNEXPECTED);
    expect(original.value).toBe(1);
    expect(original.status).toEqual(Succeeded.SUCCESS);
    expect(mapped.value).toBe(2);
  });
});
