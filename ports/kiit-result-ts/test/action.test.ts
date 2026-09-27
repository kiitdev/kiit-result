import { describe, expect, it } from "vitest";
import { Failure, Success, Action } from "../src/index.js";

// Ported from ActionTests.kt. Not ported yet: the Outcomes.attempt / Outcomes.of / Tries.attempt
// cases, which need the builders (a later phase). Kotlin's named arguments (`xid = ...`,
// `previous = ...`) become an options object: `Action("name", { xid, previous })`.

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
