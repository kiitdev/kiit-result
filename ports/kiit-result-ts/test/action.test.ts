import { describe, expect, it } from "vitest";
import { Failure, Outcomes, Success, Action, Tries } from "../src/index.js";

// Ported from ActionTests.kt. Kotlin's named arguments (`xid = ...`, `previous = ...`) become an
// options object: `Action("name", { xid, previous })`. Kotlin's `Outcomes.of(action) { op }`
// is `Outcomes.withAction(action, op)` here, since `of` is taken by the value builder.

describe("withAction", () => {
  it("attaches an action with no prior action", () => {
    const result = new Success(42).withAction(Action("chargeCard", { xid: "req-1" }));
    expect(result.action?.action).toBe("chargeCard");
    expect(result.action?.xid).toBe("req-1");
    expect(result.action?.previous).toBeUndefined();
  });

  it("chains actions by default", () => {
    const inner = new Success(42).withAction(Action("chargeCard"));
    const outer = inner.withAction(Action("processOrder"));
    expect(outer.action?.action).toBe("processOrder");
    expect(outer.action?.previous?.action).toBe("chargeCard");
  });

  it("respects an explicit previous when chaining", () => {
    const first = new Success(42).withAction(Action("createUser", { xid: "1" }));
    const second = new Success(42).withAction(
      Action("justiceLeague", { xid: "1", previous: first.action }),
      true,
    );
    expect(second.action?.action).toBe("justiceLeague");
    expect(second.action?.previous?.action).toBe("createUser");
  });

  it("can opt out of chaining", () => {
    const inner = new Success(42).withAction(Action("chargeCard"));
    const outer = inner.withAction(Action("processOrder"), false);
    expect(outer.action?.action).toBe("processOrder");
    expect(outer.action?.previous).toBeUndefined();
  });

  it("does not mutate the original result", () => {
    const original = new Success(42);
    original.withAction(Action("chargeCard"));
    expect(original.action).toBeUndefined();
  });
});

describe("action propagation", () => {
  it("survives map", () => {
    const mapped = new Success(42).withAction(Action("chargeCard")).map((n) => n + 1);
    expect(mapped.getOrNull()).toBe(43);
    expect(mapped.action?.action).toBe("chargeCard");
  });

  it("survives mapError", () => {
    const mapped = new Failure("boom").withAction(Action("chargeCard")).mapError((s) => s.length);
    expect(mapped.action?.action).toBe("chargeCard");
  });

  it("survives toOutcome", () => {
    const outcome = new Failure("boom").withAction(Action("chargeCard")).toOutcome();
    expect(outcome.action?.action).toBe("chargeCard");
  });

  it("survives toTry", () => {
    const tried = new Failure("boom").withAction(Action("chargeCard")).toTry();
    expect(tried.action?.action).toBe("chargeCard");
  });
});

describe("Action", () => {
  it("defaults data to an empty record and survives a JSON round trip", () => {
    const action = Action("chargeCard", { xid: "req-1", data: { orderId: "9" }, previous: Action("start") });
    expect(Action("plain").data).toEqual({});
    expect(JSON.parse(JSON.stringify(action))).toEqual(action);
  });
});

describe("Outcomes and Tries", () => {
  it("attempt tags a success", () => {
    const result = Outcomes.attempt(Action("chargeCard", { xid: "req-1" }), () => 42);
    expect(result.success).toBe(true);
    expect(result.getOrNull()).toBe(42);
    expect(result.action?.action).toBe("chargeCard");
    expect(result.action?.xid).toBe("req-1");
  });

  it("attempt accepts just the action name", () => {
    expect(Outcomes.attempt("chargeCard", () => 42).action?.action).toBe("chargeCard");
  });

  it("attempt tags a caught exception", () => {
    const result = Outcomes.attempt("chargeCard", () => {
      throw new Error("declined");
    });
    expect(result.success).toBe(false);
    expect(result.action?.action).toBe("chargeCard");
  });

  it("withAction tags an existing outcome without catching", () => {
    const result = Outcomes.withAction(Action("getUser", { xid: "req-2" }), () => Outcomes.attempt(() => "alice"));
    expect(result.getOrNull()).toBe("alice");
    expect(result.action?.action).toBe("getUser");
    expect(result.action?.xid).toBe("req-2");
  });

  it("withAction does not catch a throw from op", () => {
    expect(() =>
      Outcomes.withAction("getUser", () => {
        throw new Error("uncaught");
      }),
    ).toThrow("uncaught");
  });

  it("withAction chains onto an explicit previous", () => {
    const first = Outcomes.withAction("createUser", () => Outcomes.attempt(() => "alice"));
    const second = Outcomes.withAction(Action("justiceLeague", { xid: "1", previous: first.action }), () =>
      Outcomes.attempt(() => "diana"),
    );
    expect(second.action?.action).toBe("justiceLeague");
    expect(second.action?.previous?.action).toBe("createUser");
  });

  it("withAction can opt out of chaining", () => {
    const inner = Outcomes.withAction("inner", () => Outcomes.attempt(() => 1));
    const outer = Outcomes.withAction("outer", () => inner, false);
    expect(outer.action?.action).toBe("outer");
    expect(outer.action?.previous).toBeUndefined();
  });

  it("attempt keeps its plain one-argument form", () => {
    const result = Outcomes.attempt(() => 42);
    expect(result.success).toBe(true);
    expect(result.getOrNull()).toBe(42);
  });

  it("Tries.attempt still catches exceptions", () => {
    const result = Tries.attempt(() => {
      throw new Error("boom");
    });
    expect(result.success).toBe(false);
  });
});
