// Living documentation of kiit-result from real Swift, run against KiitResult.framework.
// SKIE compiles its Swift wrapper code (onEnum, etc.) into the framework's own swiftmodule.
import Foundation
import KiitResult

func check(_ condition: Bool, _ label: String) {
    guard condition else {
        fatalError("FAILED: \(label)")
    }
    print("ok: \(label)")
}

// Generics require AnyObject (Swift Int/String are value types) — use boxed KotlinInt/NSString.
// Multiple constructors bridge as distinct real initializers, no collision.
let bare = Success(value: KotlinInt(value: 42))
let withMessage = Success(value: KotlinInt(value: 42), message: "created via convenience overload")
check(bare.value?.intValue == 42, "Success(value:).value")
check(withMessage.status.message == "created via convenience overload", "Success(value:message:).status.message")

let failBare = Failure(error: "boom" as NSString)
let failMessage = Failure(error: "boom" as NSString, message: "custom failure reason")
check(failBare.error! == "boom", "Failure(error:).error")
check(failMessage.status.message == "custom failure reason", "Failure(error:message:).status.message")

// Flat, compiler-enforced exhaustive switch (Success/Failure is single-level, unlike kiit-codes'
// nested Status hierarchy). Generic over <T, E> since Success/Failure don't widen to a shared
// Result<T,E> type in Swift (Kotlin's Nothing doesn't bridge as a universal bottom type here).
func describe<T, E>(_ r: Result<T, E>) -> String {
    switch onEnum(of: r) {
    case .success(let s): return "Success: \(String(describing: s.value))"
    case .failure(let f): return "Failure: \(String(describing: f.error))"
    }
}
check(describe(bare) == "Success: Optional(42)", "describe(bare)")
check(describe(failBare) == "Failure: Optional(boom)", "describe(failBare)")

// kiit-codes' Status/Passed/Failed nested exhaustiveness carries over identically here (same
// underlying klib). Type shows as `Kiit_codesStatus` (module-prefixed, not yet @ObjCName'd) —
// naming only, access works correctly.
func describeStatus(_ status: Kiit_codesStatus) -> String {
    switch onEnum(of: status) {
    case .passed(let passed):
        switch onEnum(of: passed) {
        case .succeeded(let s): return "Succeeded: \(s.name)"
        case .pending(let p): return "Pending: \(p.name)"
        case .excluded(let e): return "Excluded: \(e.name)"
        case .information(let i): return "Information: \(i.name)"
        }
    case .failed(let failed):
        switch onEnum(of: failed) {
        case .restricted(let r): return "Restricted: \(r.name)"
        case .invalid(let i): return "Invalid: \(i.name)"
        case .rejected(let r): return "Rejected: \(r.name)"
        case .unserved(let u): return "Unserved: \(u.name)"
        }
    }
}
check(describeStatus(bare.status) == "Succeeded: SUCCESS", "describeStatus(bare.status)")

// Plain objects get clean `.shared` access out of the box — no proxy functions needed, unlike JS.
let attempted = Outcomes.shared.attempt { KotlinInt(value: 100) }
check(describe(attempted) == "Success: Optional(100)", "Outcomes.shared.attempt { }")

let some = Options.shared.some(value: KotlinInt(value: 9))
check(describe(some) == "Success: Optional(9)", "Options.shared.some(value:)")

// Overloaded Builder methods stay distinguishable as overloaded Swift methods (no @JsName needed).
let r1 = Outcomes.shared.restricted()
let r2 = Outcomes.shared.restricted(message: "denied")
check(describe(r1) == "Failure: Optional(ErrorInfo(message=The request was denied., cause=null, ref=null))", "Outcomes.shared.restricted()")
check(describe(r2) == "Failure: Optional(ErrorInfo(message=denied, cause=null, ref=null))", "Outcomes.shared.restricted(message:)")

// SKIE does not give free default-argument ergonomics here — all params required explicitly.
// Matches kiit-codes' own documented CodesToHttp finding, not a regression.
let action = Action(action: "chargeCard", xid: nil, data: [:], previous: nil)
check(action.action == "chargeCard", "Action(action:xid:data:previous:).action")

// Extension functions bridge as real dot-call methods, unlike JS's free-function shape.
let chained = bare.withAction(action: action, chain: true)
check(chained.action?.action == "chargeCard", "bare.withAction(action:chain:).action")

// Note: flatMap is not usable from Swift for constructing new results — its bridged closure type
// requires returning Result<AnyObject, AnyObject>, but a real Success/Failure never satisfies that
// (Kotlin's Nothing doesn't widen), even via forced cast. Confirmed broken, not demonstrated here.

let orElse = failBare.getOrElse { KotlinInt(value: -1) }
check((orElse as! KotlinInt).intValue == -1, "failBare.getOrElse { }")

print("All sample-swift checks passed.")
