import { describe, expect, expectTypeOf, it } from "vitest";
import { Err, Invalid, Rejected, Restricted, Succeeded, Unserved, assertNever } from "@kiitdev/codes";
import type { ErrorList, Failed, HasStatus, Passed } from "@kiitdev/codes";
import { Failure, Outcomes, Success, failure, success } from "../src/index.js";
import type { Option, Outcome, Result, Try, Validated } from "../src/index.js";

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

describe("status groups narrow with the branch", () => {
  it("is exhaustive over the four Failed groups on the failure branch", () => {
    function label(r: Result<number, string>): string {
      if (r.success) return "ok";
      switch (r.status.group) {
        case "Restricted":
          return "restricted";
        case "Invalid":
          return "invalid";
        case "Rejected":
          return "rejected";
        case "Unserved":
          return "unserved";
        default:
          return assertNever(r.status);
      }
    }
    expect(label(new Failure("x", Restricted.DENIED))).toBe("restricted");
    expect(label(new Failure("x", Invalid.BAD_REQUEST))).toBe("invalid");
    expect(label(new Failure("x", Rejected.CONFLICT))).toBe("rejected");
    expect(label(new Failure("x"))).toBe("unserved");
    expect(label(new Success(1))).toBe("ok");
  });

  it("doesn't allow value or error before narrowing", () => {
    function read(r: Result<number, string>): void {
      // @ts-expect-error `value` only exists once narrowed to Success
      r.value;
      // @ts-expect-error `error` only exists once narrowed to Failure
      r.error;
    }
    expect(read).toBeTypeOf("function");
  });
});

describe("aliases and builders", () => {
  it("defines each alias as a Result with the matching error type", () => {
    expectTypeOf<Outcome<number>>().toEqualTypeOf<Result<number, Err>>();
    expectTypeOf<Try<number>>().toEqualTypeOf<Result<number, Error>>();
    expectTypeOf<Option<number>>().toEqualTypeOf<Result<number, undefined>>();
    expectTypeOf<Validated<number>>().toEqualTypeOf<Result<number, ErrorList>>();
  });

  it("returns the concrete branch, assignable to any Result of that error type", () => {
    expectTypeOf(Outcomes.invalid("x")).toEqualTypeOf<Failure<Err>>();
    expectTypeOf(Outcomes.success(1)).toEqualTypeOf<Success<number>>();
    const anyT: Outcome<string> = Outcomes.invalid("x");
    const other: Outcome<Date> = Outcomes.invalid("x");
    expect([anyT.success, other.success]).toEqual([false, false]);
  });

  it("requires the right status group for HasStatus helpers", () => {
    class Bad implements HasStatus<Failed> {
      readonly status: Failed = Unserved.UNEXPECTED;
    }
    // @ts-expect-error a value with a Failed status can't be wrapped by success()
    success(new Bad());
    expect(failure(new Bad()).success).toBe(false);
  });
});
