import { describe, expect, it } from "vitest";
import { Err, Restricted, Succeeded, Unserved } from "@kiitdev/codes";
import { Action, Failure, Success, flatten, getOrRethrow } from "../src/index.js";
import type { Result } from "../src/index.js";
import type { Try } from "../src/index.js";
import { succeed, unserved } from "./support.js";

// Ported from ResultFunctionalTests.kt. Kotlin's `then` is `flatMap` / `andThen` here (no `then`,
// see result.ts). The Outcomes.attempt call in can_chain is replaced by a plain Success, since the
// builders come in a later phase.

describe("getOrElse / getOr / getOrNull / getErrorOrNull", () => {
  it("can get or else", () => {
    expect(succeed("peter parker").getOrElse(() => "")).toBe("peter parker");
    expect(unserved<string>("name unknown").getOrElse((err) => err.message)).toBe("name unknown");
  });

  it("can get or null", () => {
    expect(unserved<string>("name unknown").getOrNull()).toBeNull();
    expect(succeed("peter parker").getOrNull()).toBe("peter parker");
  });

  it("can get or", () => {
    expect(succeed("peter parker").getOr("??")).toBe("peter parker");
    expect(unserved<string>("name unknown").getOr("??")).toBe("??");
  });

  it("can get error or null", () => {
    expect(succeed("peter parker").getErrorOrNull()).toBeNull();
    expect(unserved<string>("name unknown").getErrorOrNull()?.message).toBe("name unknown");
  });
});

describe("exists / existsError", () => {
  it("can check exists", () => {
    expect(succeed("peter parker").exists((s) => s === "peter parker")).toBe(true);
    expect(unserved<string>("x").exists(() => true)).toBe(false);
  });

  it("can check exists error", () => {
    expect(unserved<string>("name unknown").existsError((e) => e.message === "name unknown")).toBe(true);
    expect(succeed("peter parker").existsError(() => true)).toBe(false);
  });
});

describe("getOrThrow / getErrorOrThrow", () => {
  it("can get or throw", () => {
    expect(succeed("peter parker").getOrThrow()).toBe("peter parker");
    expect(() => unserved<string>("name unknown").getOrThrow()).toThrow(Unserved.UNEXPECTED.message);
  });

  it("rethrows an Error error as-is", () => {
    const original = new Error("boom");
    const failure = new Failure(original);
    expect(() => failure.getOrThrow()).toThrow(original);
    try {
      failure.getOrThrow();
    } catch (e) {
      expect(e).toBe(original);
    }
  });

  it("builds an Error from a non-Error, non-Err error", () => {
    expect(() => new Failure("boom").getOrThrow()).toThrow("boom");
    expect(() => new Failure(null, Unserved.UNEXPECTED).getOrThrow()).toThrow(Unserved.UNEXPECTED.message);
  });

  it("can get or throw with a message, keeping the failure as the cause", () => {
    expect(succeed("peter parker").getOrThrow(() => "expected a value")).toBe("peter parker");
    const failure = unserved<string>("name unknown");
    expect(() => failure.getOrThrow(() => "expected a value")).toThrow(
      `expected a value: ${Unserved.UNEXPECTED.message}`,
    );
    try {
      failure.getOrThrow(() => "expected a value");
    } catch (e) {
      expect((e as Error).cause).toBeInstanceOf(Error);
    }
  });

  it("can get error or throw", () => {
    expect(unserved<string>("name unknown").getErrorOrThrow().message).toBe("name unknown");
    expect(() => succeed("peter parker").getErrorOrThrow()).toThrow("getErrorOrThrow() called on a Success value");
  });

  it("can get error or throw with a message", () => {
    expect(unserved<string>("name unknown").getErrorOrThrow(() => "expected a failure").message).toBe("name unknown");
    expect(() => succeed("peter parker").getErrorOrThrow(() => "expected a failure")).toThrow(
      "expected a failure: peter parker",
    );
  });
});

describe("recover", () => {
  it("keeps a Success as-is", () => {
    expect(succeed("peter parker").recover(() => "???").getOrNull()).toBe("peter parker");
  });

  it("turns a Failure into a Success, keeping the action", () => {
    const failure = unserved<string>("name unknown").withAction(Action("lookupName"));
    const recovered = failure.recover(() => "???");
    expect(recovered.getOrNull()).toBe("???");
    expect(recovered.status).toEqual(Succeeded.SUCCESS);
    expect(recovered.action?.action).toBe("lookupName");
  });
});

describe("map / flatMap / andThen", () => {
  it("maps the success branch", () => {
    const result = succeed("peter parker").map((name) => `${name} : spider-man`);
    expect(result.getOrElse(() => "")).toBe("peter parker : spider-man");
  });

  it("flatMaps the success branch", () => {
    const result = succeed("peter parker").flatMap((name) => succeed(`${name} : spider-man`));
    expect(result.getOrElse(() => "")).toBe("peter parker : spider-man");
  });

  it("andThen is an alias of flatMap", () => {
    const result = succeed("peter parker").andThen((name) => succeed(`${name} : spider-man`));
    expect(result.getOrElse(() => "")).toBe("peter parker : spider-man");
  });

  it("onSuccess sees the mapped value", () => {
    let seen = "";
    succeed("peter parker")
      .map((name) => `${name} : spider-man`)
      .onSuccess((v) => {
        seen = v;
      });
    expect(seen).toBe("peter parker : spider-man");
  });

  it("map on a failure keeps the failure, status and message", () => {
    const result = unserved<string>("name unknown").map((name) => `${name} : spider-man`);
    expect(result.success).toBe(false);
    expect(result.status).toEqual(Unserved.UNEXPECTED);
    expect(result.message).toBe(Unserved.UNEXPECTED.message);
    expect(result.getOrElse(() => "??")).toBe("??");
  });

  it("flatMap on a failure short-circuits, without calling f", () => {
    let called = false;
    const result = unserved<string>("name unknown").flatMap((name) => {
      called = true;
      return succeed(name);
    });
    expect(called).toBe(false);
    expect(result.success).toBe(false);
    expect(result.status).toEqual(Unserved.UNEXPECTED);
  });
});

describe("mapError / orElse / or / and", () => {
  it("converts the error via mapError", () => {
    const result = unserved<string>("name unknown").mapError(() => 0);
    expect(result.success).toBe(false);
    expect(result.status).toEqual(Unserved.UNEXPECTED);
    let seen = -1;
    result.onFailure((e) => {
      seen = e;
    });
    expect(seen).toBe(0);
  });

  it("converts the error via orElse", () => {
    const result = unserved<string>("name unknown").orElse(() => new Failure(0));
    expect(result.success).toBe(false);
    expect(result.status).toEqual(Unserved.UNEXPECTED);
    expect(result.getErrorOrNull()).toBe(0);
  });

  it("orElse can recover into a Success", () => {
    expect(unserved<string>("x").orElse(() => new Success("fallback")).getOrNull()).toBe("fallback");
    expect(succeed("ok").orElse(() => new Success("fallback")).getOrNull()).toBe("ok");
  });

  it("or keeps a Success and falls back otherwise", () => {
    expect(succeed(1).or(succeed(2)).getOrNull()).toBe(1);
    expect(unserved<number>("x").or(succeed(2)).getOrNull()).toBe(2);
  });

  it("and returns other for a Success and stays failed otherwise", () => {
    expect(succeed(1).and(succeed("b")).getOrNull()).toBe("b");
    expect(unserved<number>("x").and(succeed("b")).success).toBe(false);
  });
});

describe("transform / fold", () => {
  it("can transform", () => {
    const result = succeed("peter parker").transform(
      (name) => new Success(`${name} : spider-man`),
      () => new Failure("a marvel character"),
    );
    expect(result.success).toBe(true);
    expect(result.status).toEqual(Succeeded.SUCCESS);
    expect(result.message).toBe(Succeeded.SUCCESS.message);
    expect(result.getOrNull()).toBe("peter parker : spider-man");
  });

  it("can transform with fold", () => {
    const name = succeed("peter parker").fold(
      (n) => `${n} : spider-man`,
      () => "a marvel character",
    );
    expect(name).toBe("peter parker : spider-man");
    expect(unserved<string>("x").fold((n) => n, (e) => e.message)).toBe("x");
  });
});

describe("withStatus", () => {
  it("replaces the status of the branch it's on", () => {
    expect(new Success(1).withStatus(Succeeded.CREATED, Unserved.TIMEOUT).status).toEqual(Succeeded.CREATED);
    expect(new Failure("x").withStatus(Succeeded.CREATED, Unserved.TIMEOUT).status).toEqual(Unserved.TIMEOUT);
  });
});

describe("chaining", () => {
  it("can chain", () => {
    let successValue = "";
    const result = succeed("1")
      .map((s) => Number.parseInt(s, 10))
      .onSuccess((n) => {
        successValue = `converted to int: ${n}`;
      })
      .flatMap((n) => new Success(n + 1));

    expect(successValue).toBe("converted to int: 1");
    expect(result.exists((n) => n === 2)).toBe(true);
    expect(result.fold((n) => `final value: ${n}`, (e) => `error : ${String(e)}`)).toBe("final value: 2");
  });

  it("carries an Err through a chain", () => {
    const err = Err.of("boom");
    const result = succeed(1)
      .flatMap((): Result<number, Err> => new Failure(err))
      .map((n) => n + 1);
    expect(result.getErrorOrNull()).toBe(err);
  });
});

describe("getOrRethrow", () => {
  it("returns the value of a Success", () => {
    const ok: Try<number> = new Success(42);
    expect(getOrRethrow(ok)).toBe(42);
  });

  it("throws the original Error, same instance", () => {
    const original = new Error("boom");
    const bad: Try<number> = new Failure(original);
    expect(() => getOrRethrow(bad)).toThrow(original);
    try {
      getOrRethrow(bad);
    } catch (e) {
      expect(e).toBe(original);
    }
  });
});

describe("flatten", () => {
  it("flattens a nested Success", () => {
    const nested: Result<Result<string, Err>, Err> = new Success(new Success("peter parker"));
    expect(flatten(nested).getOrNull()).toBe("peter parker");
  });

  it("flattens a Failure inside a Success", () => {
    const inner = new Failure(Err.of("boom"));
    const nested: Result<Result<string, Err>, Err> = new Success(inner);
    expect(flatten(nested)).toBe(inner);
  });

  it("returns an outer Failure with its status and action intact", () => {
    const outer: Result<Result<string, Err>, Err> = new Failure(Err.of("boom"), Restricted.DENIED).withAction(
      Action("lookup"),
    );
    const flat = flatten(outer);
    expect(flat.status).toEqual(Restricted.DENIED);
    expect(flat.action?.action).toBe("lookup");
  });
});
