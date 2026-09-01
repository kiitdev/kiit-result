# Changelog

All notable changes to kiit-result are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versions follow
[Semantic Versioning](https://semver.org/).

## [1.0.1] - 2026-08-31

### Added
- `getErrorOrNull`, `existsError`, `getOr` — direct accessors mirroring `getOrNull`/`exists`/`getOrElse`.
- `getOrThrow`, `getOrThrow(message)`, `getErrorOrThrow`, `getErrorOrThrow(message)`, `getOrRethrow` — throwing escape hatches for tests, prototypes, and exception-only boundaries.
- `recover` — unconditionally turns a `Failure` into a `Success`.
- `List<Result<T, E>>.combine()`/`.partition()`/`.allSuccess()`/`.allFailure()`/`.anySuccess()`/`.anyFailure()` — batch operators for working with a list of `Result`s.

### Changed
- `getOrElse` now passes the failure's `error` to its fallback function, matching Kotlin's own `kotlin.Result.getOrElse`, kotlin-result, and Rust.
- `inner` renamed to `flatten`, matching the name both Rust (stable since 1.89.0) and kotlin-result already use for the same operation.
- Extension operators moved out of `Result.kt` into their own files (`ResultOps.kt`, `ResultListOps.kt`), since Kotlin's declaration-site variance rules require some of them to be top-level functions rather than members.
- Bumped the `kiit-codes` dependency to 1.0.2.
- README restructured to match `kiit-codes`: the FAQ moved to the docs site, the Learn More table now points at real anchors, and "framework" became "toolkit" throughout.

### Removed
- `operate` — redundant with `flatMap`/`then` (the only difference, access to the whole `Result` instead of just the value, is already achievable by reading `status`/`action` into a local variable before calling `flatMap`). Never had any call sites in this repo.

### Fixed
- `toOutcome()` no longer discards the original status when a `Failure`'s error is `null`. It now derives the `Err`'s message from the actual status instead of a hardcoded generic one.
- `FailedBuilder`'s `restricted`/`invalid`/`unserved` overloads now thread an explicit `status` argument through to `errorFromEx`/`errorFromErr` consistently, matching `rejected`'s existing behavior.
- Stale KDoc examples for `getOrElse` and `inner` corrected to match actual behavior.

## [1.0.0] - 2026-08-15

### Changed
- Finalization after reviews, tests, sample code

## [0.x.x] - 2026-07-01

### Added
- Extracted from the Kiit framework as its own standalone module.
