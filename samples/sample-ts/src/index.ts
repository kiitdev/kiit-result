/**
 * Living documentation of kiit-result from real TypeScript, type-checked (`npm run typecheck`)
 * against the actual generated `.d.ts` at ../../kiit-result/build/dist/js/productionLibrary — not
 * a hand-written stub. Kept for future reference in case native TS/JS support becomes a priority
 * again; not wired into CI (see .github/workflows/ci.yml) or published to npm for now.
 *
 * Two things worth knowing before reading this:
 *
 * 1. kiit-result depends on kiit-codes' *published* JS artifact, which only exports a handful of
 *    jsMain-only wrapper classes (RestrictedError etc.) — its core Status/Passed/Failed/Err types
 *    aren't exported yet, so they render as `any` below (e.g. Result.status). This isn't just a
 *    typing gap — confirmed at runtime too: an unexported Kotlin class's properties are mangled
 *    (`status.name` on a real `Succeeded` instance is `undefined`; the actual object has fields
 *    like `s9_1`/`t9_1`/`u9_1` instead), so `status`/`error` access below is deliberately avoided
 *    beyond what's shown. This sample sticks to the bare/message-only constructor forms, which
 *    don't need a real Passed/Failed instance. Revisit once kiit-codes republishes with its own JS
 *    export pass — at that point `status.name` etc. should resolve correctly.
 *
 * 2. kiit.result.builders.Builder/PassedBuilder/FailedBuilder are exported as *types* (their
 *    success/restricted/invalid/rejected/unserved methods are all here, @JsName-disambiguated),
 *    but nothing that implements them is exported — Outcomes/Options/Tries are plain Kotlin
 *    `object`s, which can't export usably to JS/TS (confirmed empirically; see kiit-result's build
 *    notes). So these interfaces type-check but have no reachable instance from TypeScript today —
 *    not demonstrated below for that reason.
 */
import { kiit } from "@kiit/result";

const { Success, Failure, Action, withAction } = kiit.result;

function assertNever(x: never): never {
    throw new Error("Unexpected object: " + x);
}

// Kotlin secondary constructors can't become alternate `new X(...)` forms in JS — JS classes
// support exactly one constructor. They're exported as `static` factory methods instead:
// Success.of(value) / Success.ofMessage(value, msg), not `new Success(value)`.
const bare = Success.of(42);
const withMsg = Success.ofMessage(42, "created via convenience factory");
console.log("bare:", bare.value, "withMsg:", withMsg.value);
// bare.status exists and type-checks (as `any`), but its fields are currently unreadable at
// runtime — see the module comment above. Not printed here for that reason.

const failBare = Failure.of("boom");
const failWithMsg = Failure.ofMessage("boom", "custom failure reason");
console.log("failBare:", failBare.error, "failWithMsg:", failWithMsg.error);

// TypeScript has no compiler-enforced exhaustiveness over Kotlin sealed hierarchies — the .d.ts
// emits independent `class` declarations, never a `type X = A | B` union. instanceof narrowing +
// an assertNever fallback is the best available idiom (same finding kiit-codes documented in its
// own sample-ts).
function describe(result: InstanceType<typeof Success<number>> | InstanceType<typeof Failure<string>>): string {
    if (result instanceof Success) {
        return `Success: ${result.value}`;
    } else if (result instanceof Failure) {
        return `Failure: ${result.error}`;
    }
    return assertNever(result);
}

console.log("describe(bare):", describe(bare));
console.log("describe(failBare):", describe(failBare));

// Action / withAction — Kotlin's native default parameters (not @JvmOverloads, which is JVM-only)
// render as genuine optional TS params, so `new Action("chargeCard")` works directly with no
// overload multiplication needed the way Java requires.
const action = new Action("chargeCard");
const chained = withAction(bare, action);
console.log("chained action:", chained.action?.action);

// Extension functions render as flat top-level functions taking the receiver as an explicit first
// parameter, not as callable methods on the value — kiit.result.flatMap(result, fn), not
// result.flatMap(fn).
const mapped = kiit.result.flatMap(bare, (v) => Success.of(v + 1));
if (mapped instanceof Success) {
    console.log("mapped:", mapped.value);
}

const orElse = kiit.result.getOrElse(failBare, () => -1);
console.log("orElse:", orElse);

// Result.attempt / Result.outcome — @JsStatic promotes companion members to real `static` methods
// directly on the exported class, exactly like Kotlin's own `Result.attempt(...)` call syntax.
const tried = kiit.result.Result.attempt(() => 5);
if (tried instanceof Success) {
    console.log("tried:", tried.value);
}
