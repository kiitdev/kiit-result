import { describe, expect, expectTypeOf, it } from "vitest";
import { RestrictedError, Restricted, Succeeded, Unserved } from "@kiitdev/codes";
import type { Err } from "@kiitdev/codes";
import { Action, Failure, Options, Outcomes, Success, Tries } from "../src/index.js";
import type { Outcome, Result } from "../src/index.js";
import { succeed, unserved } from "./support.js";

// Ported from CoroutineCompositionTest.kt. Kotlin's `inline` lets a `suspend` call sit inside
// map/flatMap/onSuccess lambdas. In TS a sync `map` with an async callback would give a
// Result<Promise<T>>, so the async callbacks go through mapAsync / flatMapAsync / onSuccessAsync.
// The rest is TS-only: rejection handling, actions, status, and short-circuiting.

async function fetchEmail(id: string): Promise<string> {
  if (id.trim() === "") throw new Error("blank id");
  return `${id}@example.com`;
}

describe("async callbacks", () => {
  it("mapAsync runs an async call inside its callback", async () => {
    const result = await succeed("u1").mapAsync((id) => fetchEmail(id));
    expect(result.success).toBe(true);
    expect(result.getOrNull()).toBe("u1@example.com");
  });

  it("flatMapAsync chains an async step that returns a Result", async () => {
    const result = await succeed("u1").flatMapAsync(async (id) => Outcomes.success(await fetchEmail(id)));
    expect(result.getOrNull()).toBe("u1@example.com");
  });

  it("onSuccessAsync runs an async side effect and waits for it", async () => {
    let captured: string | undefined;
    const original = succeed("u1");
    const returned = await original.onSuccessAsync(async (id) => {
      captured = await fetchEmail(id);
    });
    expect(captured).toBe("u1@example.com");
    expect(returned).toBe(original);
  });

  it("onFailureAsync runs an async side effect only on a failure", async () => {
    const seen: string[] = [];
    await unserved<string>("boom").onFailureAsync(async (e) => {
      seen.push(e.message);
    });
    await succeed("ok").onFailureAsync(async () => {
      seen.push("should not run");
    });
    expect(seen).toEqual(["boom"]);
  });
});

describe("attemptAsync", () => {
  it("wraps an async call and chains another step", async () => {
    const result = (await Outcomes.attemptAsync(() => fetchEmail("u1"))).flatMap((email) =>
      Outcomes.success(email.toUpperCase()),
    );
    expect(result.getOrNull()).toBe("U1@EXAMPLE.COM");
  });

  it("captures a rejection as an Err", async () => {
    const result: Outcome<string> = await Outcomes.attemptAsync(() => fetchEmail(""));
    expect(result.success).toBe(false);
    expect(result.status).toEqual(Unserved.UNEXPECTED);
    result.onFailure((err: Err) => expect(err.message).toBe("blank id"));
  });

  it("captures a synchronous throw from a non-async function", async () => {
    const result = await Outcomes.attemptAsync((): Promise<number> => {
      throw new Error("sync boom");
    });
    expect(result.getErrorOrNull()?.message).toBe("sync boom");
  });

  it("accepts a plain value as well as a promise", async () => {
    expect((await Outcomes.attemptAsync(() => 42)).getOrNull()).toBe(42);
  });

  it("captures a rejected non-Error value", async () => {
    const result = await Outcomes.attemptAsync(() => Promise.reject("just a string"));
    expect(result.getErrorOrNull()?.message).toBe("just a string");
  });

  it("tags the result with an action, on success and failure", async () => {
    const ok = await Outcomes.attemptAsync("fetchEmail", () => fetchEmail("u1"));
    expect(ok.action?.action).toBe("fetchEmail");
    const bad = await Outcomes.attemptAsync(Action("fetchEmail", { xid: "req-1" }), () => fetchEmail(""));
    expect(bad.success).toBe(false);
    expect(bad.action?.xid).toBe("req-1");
  });

  it("Tries.attemptAsync keeps the rejected Error, and a StatusError's status", async () => {
    const error = new Error("boom");
    const tried = await Tries.attemptAsync(() => Promise.reject(error));
    expect(tried.getErrorOrNull()).toBe(error);

    const restricted = new RestrictedError(Restricted.UNAUTHORIZED);
    const status = await Tries.attemptAsync(() => Promise.reject(restricted));
    expect(status.status).toEqual(Restricted.UNAUTHORIZED);
    expect(status.getErrorOrNull()).toBe(restricted);
  });

  it("Options.attemptAsync gives None for a rejection", async () => {
    expect((await Options.attemptAsync(() => fetchEmail("u1"))).getOrNull()).toBe("u1@example.com");
    const none = await Options.attemptAsync(() => fetchEmail(""));
    expect(none.success).toBe(false);
    expect(none.status).toEqual(Unserved.UNEXPECTED);
  });
});

describe("async operators keep status, action and short-circuit", () => {
  it("mapAsync keeps the success status and action", async () => {
    const tagged = new Success("u1", Succeeded.CREATED).withAction(Action("create"));
    const mapped = await tagged.mapAsync((id) => fetchEmail(id));
    expect(mapped.status).toEqual(Succeeded.CREATED);
    expect(mapped.action?.action).toBe("create");
  });

  it("mapAsync and flatMapAsync skip the callback on a failure, keeping it as-is", async () => {
    let calls = 0;
    const failure = unserved<string>("boom");
    const mapped = await failure.mapAsync(async (id) => {
      calls++;
      return id;
    });
    const flatMapped = await failure.flatMapAsync(async (id) => {
      calls++;
      return succeed(id);
    });
    expect(calls).toBe(0);
    expect(mapped).toBe(failure);
    expect(flatMapped).toBe(failure);
  });

  it("flatMapAsync returns the inner Failure", async () => {
    const result = await succeed("u1").flatMapAsync(async () => unserved<string>("inner"));
    expect(result.getErrorOrNull()?.message).toBe("inner");
  });

  it("a rejection inside an async callback rejects the promise, it isn't caught", async () => {
    await expect(succeed("").mapAsync((id) => fetchEmail(id))).rejects.toThrow("blank id");
    await expect(succeed("").flatMapAsync(async (id) => succeed(await fetchEmail(id)))).rejects.toThrow("blank id");
  });

  it("types the results", async () => {
    const mapped = await new Success(1).mapAsync(async (n) => String(n));
    expectTypeOf(mapped).toEqualTypeOf<Result<string, never>>();
    const flat = await succeed(1).flatMapAsync(async (n) => new Failure(new Error(String(n))) as Result<number, Error>);
    const widened: Result<number, Err | Error> = flat; // both error types are tracked
    expect(widened.success).toBe(false);
  });
});
