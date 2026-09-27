import { describe, expect, it } from "vitest";
import { Succeeded, Unserved } from "@kiitdev/codes";

// Phase 1 smoke test: proves the vitest + @kiitdev/codes link work. Deleted in Phase 2.
describe("scaffold", () => {
  it("resolves @kiitdev/codes", () => {
    expect(Succeeded.SUCCESS.success).toBe(true);
    expect(Unserved.UNEXPECTED.success).toBe(false);
  });
});
