/**
 * Living documentation of kiit-result from real TypeScript, type-checked (`npm run typecheck`)
 * against the actual generated `.d.ts`, not a hand-written stub. Kept for future reference in
 * case native TS/JS support becomes a priority again, not wired into CI or published to npm.
 *
 * A couple of things worth knowing:
 * 1. kiit-codes' core Status/Passed/Failed/Err types aren't exported to JS yet, so they render
 *    as `any` below (e.g. Result.status). `status`/`error` access is deliberately avoided beyond
 *    what's shown, this sample sticks to the bare/message-only constructor forms instead.
 * 2. `Builder`/`PassedBuilder`/`FailedBuilder` type-check but have no reachable instance from
 *    TypeScript, since Outcomes/Options/Tries are plain Kotlin `object`s that can't export
 *    usably to JS/TS. Not demonstrated below for that reason.
 */
import { kiit } from "@kiit/result";

const { Success, Failure, Action } = kiit.result;

function assertNever(x: never): never {
    throw new Error("Unexpected object: " + x);
}

// Kotlin secondary constructors can't become alternate `new X(...)` forms in JS — JS classes
// support exactly one constructor. They're exported as `static` factory methods instead:
// Success.of(value) / Success.ofMessage(value, message), not `new Success(value)`.
const bare = Success.of(42);
const withMessage = Success.ofMessage(42, "created via convenience factory");
console.log("bare:", bare.value, "withMessage:", withMessage.value);
// bare.status exists and type-checks (as `any`), but its fields are currently unreadable at
// runtime, see the module comment above. Not printed here for that reason.

const failBare = Failure.of("boom");
const failWithMessage = Failure.ofMessage("boom", "custom failure reason");
console.log("failBare:", failBare.error, "failWithMessage:", failWithMessage.error);

// TypeScript has no compiler-enforced exhaustiveness over Kotlin sealed hierarchies. instanceof
// narrowing plus an assertNever fallback is the best available idiom instead (same finding
// kiit-codes documented in its own sample-ts).
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

// Action: Kotlin's native default parameters render as genuine optional TS params, so
// `new Action("chargeCard")` works directly with no overload multiplication needed the way
// Java requires. withAction is a member, not an extension, so it's a real method call here too,
// unlike flatMap/getOrElse below.
const action = new Action("chargeCard");
const chained = bare.withAction(action);
console.log("chained action:", chained.action?.action);

// Extension functions render as flat top-level functions taking the receiver as an explicit first
// parameter, not as callable methods on the value: kiit.result.flatMap(result, fn), not
// result.flatMap(fn).
const mapped = kiit.result.flatMap(bare, (v) => Success.of(v + 1));
if (mapped instanceof Success) {
    console.log("mapped:", mapped.value);
}

const orElse = kiit.result.getOrElse(failBare, () => -1);
console.log("orElse:", orElse);

// Result.attempt / Result.outcome: @JsStatic promotes companion members to real `static` methods
// directly on the exported class, exactly like Kotlin's own `Result.attempt(...)` call syntax.
const tried = kiit.result.Result.attempt(() => 5);
if (tried instanceof Success) {
    console.log("tried:", tried.value);
}
