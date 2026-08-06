<div align="center">

# kiit.result

A type for representing the result of an operation, capturing either a success or failure with a value or error details, with optional support for a taxonomy of status codes.

[![Maven Central](https://img.shields.io/maven-central/v/dev.kiit/kiit-result?color=blue)](https://central.sonatype.com/artifact/dev.kiit/kiit-result)
[![Build](https://img.shields.io/github/actions/workflow/status/slatekit/kiit-result/ci.yml?branch=main)](https://github.com/slatekit/kiit-result/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/slatekit/kiit-result)](./LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-purple.svg)](https://kotlinlang.org)

Part of the [Kiit](https://www.kiit.dev) framework · [kiit.dev/result](https://www.kiit.dev/result) · [Blog post](#) · [Video walkthrough](#)

</div>

---

## 📚 Table of Contents

| Topic | Description |
|---|---|
| **Overview** | |
| ℹ️ [About](#ℹ️-about) | What kiit.result is and how it relates to kiit-codes |
| 🧩 [The problem](#-the-problem) | Why exceptions and nullable returns don't compose well for expected failure |
| 💡 [The idea](#-the-idea) | A `Result<T, E>` monad built directly on kiit-codes' status taxonomy |
| **Start** | |
| 🚀 [Quick start](#-quick-start) | Install the library and see `Outcome`, builders, and conversions |
| **Reference** | |
| 🧠 [Core concepts](#-core-concepts) | `Result`/`Success`/`Failure`, the type aliases, and the builders |
| 🏗️ [Builders](#️-builders) | The status-aware `restricted`/`invalid`/`rejected`/`unserved`/`excluded` factories |
| 🔁 [Conversions](#-conversions) | `toOutcome()`/`toTry()` and interop with kiit-codes' `StatusException` |
| **Guidance** | |
| 🛠️ [Use cases](#️-use-cases) | Where this fits — services, pipelines, validation |
| ✅ [When to use this](#-when-to-use-this-and-when-not-to) | Good-fit and not-necessary scenarios |
| **Project** | |
| 📦 [Requirements](#-requirements) | Supported platforms and dependencies |
| 🗺️ [Roadmap](#️-roadmap) | Publishing pipelines and CI work planned but not yet done |
| 🤝 [Contributing](#-contributing) | How to build, test, and submit changes |
| 📄 [License](#-license) | Licensing terms for this project |

---

## ℹ️ About

**kiit.result** is a `Result<T, E>` type for Kotlin Multiplatform — similar to `Result` in Rust and Swift, or `Try` in Scala. It models the outcome of an operation as one of two branches, `Success<T>` or `Failure<E>`, each carrying an optional [kiit-codes](https://github.com/slatekit/kiit-codes) `Status` so a caller can inspect *why*, not just *whether*.

It builds directly on kiit-codes rather than reimplementing status classification:

1. **A monadic `Result<T, E>`** — `map`, `flatMap`/`then`, `fold`, `onSuccess`/`onFailure`, `getOrElse`, and friends, so success/failure handling composes without manual `if`/`else` branching.
2. **A flexible error type** — the `Failure` branch's error type `E` can be anything: `String`, `Throwable`, [kiit-codes](https://github.com/slatekit/kiit-codes)' `Err`, or your own domain type. Type aliases (`Try<T>`, `Option<T>`, `Outcome<T>`) cover the common cases.
3. **Status-aware builders** — `restricted`/`invalid`/`rejected`/`unserved`/`excluded` build a `Result` pre-populated with the matching kiit-codes status category, so you rarely construct `Success`/`Failure` by hand.

```kotlin
val outcome: Outcome<User> = userService.create("alice", "alice@example.com")
outcome.fold(
    { user -> println("created ${user.id}") },
    { err -> println("failed: ${err.msg} (${outcome.status.name})") },
)
```

## 🧩 The problem

Returning `null` for "not found" loses the reason. Throwing for expected, recoverable failures (validation, a conflict, an unauthorized caller) is expensive and easy to over- or under-catch. And once you *do* return a status/error pair by convention, every caller ends up re-deriving the same success/failure branching logic by hand.

## 💡 The idea

**A `Result<T, E>` that composes the usual monadic operations with kiit-codes' closed status taxonomy**, instead of a bespoke or numeric status of its own. `Success` carries a [kiit-codes](https://github.com/slatekit/kiit-codes) `Passed` status (`Succeeded`, `Pending`, `Excluded`, `Information`); `Failure` carries a `Failed` status (`Restricted`, `Invalid`, `Rejected`, `Unserved`). Builders map each common case to its matching category, so `restricted()` gives you `Restricted.DENIED`, `invalid()` gives you `Invalid.INVALID_VALUE`, and so on — without hand-rolling a status object at every call site.

## 🚀 Quick start

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("dev.kiit:kiit-result:0.1.0")
}
```

`kiit-result` depends on `dev.kiit:kiit-codes` transitively — you don't need to add it separately.

**Return an `Outcome<T>` (`Result<T, Err>`) using the builder methods:**

```kotlin
import kiit.codes.Invalid
import kiit.codes.Rejected
import kiit.result.Outcome
import kiit.result.OutcomeBuilder

class UserService : OutcomeBuilder {
    private val users = mutableMapOf<String, User>()

    fun create(id: String, email: String): Outcome<User> {
        if (email.isBlank()) return invalid(Invalid.BAD_REQUEST)
        if (users.containsKey(id)) return rejected(Rejected.CONFLICT)
        val user = User(id, email)
        users[id] = user
        return success(user)
    }
}
```

**Compose with `map`/`flatMap`/`fold`:**

```kotlin
import kiit.result.flatMap

userService.create("alice", "alice@example.com")
    .map { it.email }
    .onSuccess { println("registered: $it") }
    .onFailure { err -> println("could not register: ${err.msg}") }
```

**Convert to a `Try<T>` to cross an exception-only boundary:**

```kotlin
// Wraps a Failure<Err> into a Failure<StatusException> from kiit-codes
val asTry = userService.fetch("missing").toTry()
asTry.onFailure { ex -> println("caught: ${ex.message}") }
```

See [`samples/sample1`](./samples/sample1) for a runnable end-to-end example.

## 🧠 Core concepts

```
Result<T, E> = Success<T> | Failure<E>

Success<T>.status : Passed  (from kiit-codes)
Failure<E>.status : Failed  (from kiit-codes)
```

| Term | What it is |
|---|---|
| **`Result<T, E>`** | Sealed type, either `Success<T>` or `Failure<E>`. |
| **`Success<T>`** | Holds a `value: T` and a `status: Passed`. Defaults to `Succeeded.SUCCESS`. |
| **`Failure<E>`** | Holds an `error: E` and a `status: Failed`. Defaults to `Unserved.UNEXPECTED`. |
| **`message`** | `result.status.message` — a convenience accessor on every `Result`. |
| **`Option<T>`** | `Result<T, Unit>` — the historical role of `Option`/`Maybe` (Rust/Scala/Arrow), reimagined on `Result` so absence carries a `status` explaining why, not just a bare `None`. `Options.some(value)`/`Options.none()` are the discoverable entry points. |
| **`Try<T>`** | `Result<T, Throwable>` — exception as the error type. |
| **`Outcome<T>`** | `Result<T, Err>` — [kiit-codes](https://github.com/slatekit/kiit-codes)' `Err` as the error type; the most commonly used alias. |
| **`Validated<T>`** | `Result<T, Err.ErrorList>` — for validation, collecting multiple errors. |

Composition operators mirror what you'd expect from `Result`/`Either` in other languages: `map`, `mapError`, `flatMap`/`then`, `fold`, `exists`, `getOrNull`, `getOrElse`, `onSuccess`, `onFailure`, `transform`, `contains`, `inner` (flattens a nested `Result`), plus `or`/`and`/`operate` for combining two `Result`s.

## 🏗️ Builders

`Builder<E>` provides status-aware factory methods so you rarely build `Success`/`Failure` directly. It's composed from two smaller interfaces, one per branch, so each stays scoped to its own category constants (the same reason kiit-codes keeps `Succeeded`/`Restricted`/etc. constants on their own companions rather than one shared object):

- **`PassedBuilder<E>`** — `success`/`pending`/`excluded`, each with 3 overloads: no-arg, `(value, msg: String? = null)`, and `(value, status)`.
- **`FailedBuilder<E>`** — `restricted`/`invalid`/`rejected`/`unserved`, each with 5 overloads: no-arg, `(msg)`, `(ex, status?)`, `(err, status?)`, `(status)`.

| Builder | Status category | Default |
|---|---|---|
| `success(value)` | `Passed.Succeeded` | `Succeeded.SUCCESS` |
| `pending(value)` | `Passed.Pending` | `Pending.ACCEPTED` |
| `excluded(value)` | `Passed.Excluded` | `Excluded.SKIPPED` |
| `restricted(...)` | `Failed.Restricted` | `Restricted.DENIED` |
| `invalid(...)` | `Failed.Invalid` | `Invalid.INVALID_VALUE` |
| `rejected(...)` | `Failed.Rejected` | `Rejected.RULE_VIOLATION` |
| `unserved(...)` | `Failed.Unserved` | `Unserved.UNEXPECTED` |

Note that `excluded()` builds a **`Success`**, not a `Failure` — an intentionally excluded/skipped item (deduplicated, disqualified, filtered out) is a [kiit-codes](https://github.com/slatekit/kiit-codes) `Passed.Excluded` status, not a failure. There's no separate `conflict()` — it's `rejected(status = Rejected.CONFLICT)`, since a conflict is just a specific `Rejected` outcome, not its own category.

`Options` also adds `some(value)`/`none(...)` on top of the generic builders above — a discoverable `Some`/`None`-style pair for `Option<T>` specifically (see [Core concepts](#-core-concepts)). `none()` defaults to `Rejected.NOT_EXISTS`, distinct from the generic `Unserved.UNEXPECTED` fallback:

```kotlin
import kiit.result.Options

val a = Options.some(42)                    // Option<Int> — present
val b = Options.none<Int>()                 // Option<Int> — absent, Rejected.NOT_EXISTS
val c = Options.none<Int>(Rejected.CONFLICT) // Option<Int> — absent, custom status
```

`Outcomes`/`Options`/`Tries` are the three ready-made `Builder` implementations, one per common error type:

```kotlin
import kiit.result.Outcomes
import kiit.result.Options
import kiit.result.Tries

val a = Outcomes.of { riskyCall() }   // Outcome<T>  — catches Throwable, wraps as Err
val b = Options.of { riskyCall() }    // Option<T>   — catches Throwable, discards detail
val c = Tries.of { riskyCall() }      // Try<T>      — catches Throwable, re-derives status
                                        //               from a thrown kiit-codes StatusException
```

## 🔁 Conversions

- **`toOutcome()`** — converts any `Result<T, E>` to `Outcome<T>` (`Result<T, Err>`), building an `Err` from whatever the failure held (`String`, `Exception`, or an existing `Err`).
- **`toTry()`** — converts any `Result<T, E>` to `Try<T>` (`Result<T, Throwable>`). An `Err`-typed failure becomes a [kiit-codes](https://github.com/slatekit/kiit-codes) `StatusException` via `Failed.toException(errors)`, so the exception still carries the original status and error detail.
- **`Tries.of { ... }`** — the reverse direction: if the block throws a `StatusException` (`RestrictedException`/`InvalidException`/`RejectedException`/`UnservedException`), the resulting `Try` is built with the matching `restricted`/`invalid`/`rejected`/`unserved` status instead of a generic failure.

## 🛠️ Use cases

1. **Service layers** — return `Outcome<T>` instead of throwing for expected failures.
2. **Pipelines** — `map`/`flatMap` chains compose without manual null/exception checks at each step.
3. **Validation** — `Validated<T>` (`Result<T, Err.ErrorList>`) collects multiple errors.
4. **Exception boundaries** — `toTry()`/`Tries.of` interop with `StatusException` when a caller only understands exceptions.
5. **HTTP/gRPC responses** — `result.status` converts via kiit-codes' `CodesToHttp`/`CodesToGrpc`.

## ✅ When to use this and when not to

**Good fit if:**
1. You want explicit, monadic return values instead of throw/catch for expected failures.
2. You're already using (or want) kiit-codes' status taxonomy and want a `Result` type layered on top of it, instead of a bespoke one.
3. You need to compose several fallible steps (`map`/`flatMap`) without nested `try`/`catch`.

**Probably not necessary if:**
1. Exceptions already communicate everything you need, and you don't want the monadic-return-value style.
2. You only need status classification, not a `Result` wrapper — in which case see [kiit-codes](https://github.com/slatekit/kiit-codes) on its own.

## 📦 Requirements

1. Kotlin Multiplatform — JVM, Android, JS (IR), iOS (arm64, simulator arm64, x64)
2. Depends on `dev.kiit:kiit-codes` (transitively available to consumers via `api`)

## 🗺️ Roadmap

- [ ] npm publish pipeline for JS consumers (`@kiit/result`)
- [ ] SPM / XCFramework pipeline for Swift consumers
- [ ] Diagrams and a fuller FAQ, matching kiit-codes' README

Track progress or open a discussion in [Issues](https://github.com/slatekit/kiit-result/issues).

## 🤝 Contributing

Contributions are welcome — see [BUILD.md](./BUILD.md) for build, test, and publish instructions.

## 📄 License

[Apache License 2.0](./LICENSE)

---

<div align="center">

kiit.result is one module of **[Kiit](https://www.kiit.dev)** — a lightweight, modular, 100% Kotlin framework for building server apps, APIs, CLIs, and jobs. Adopt one module at a time.

</div>
