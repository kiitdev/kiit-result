# @kiitdev/result

A `Result<T, E>` for TypeScript where every `Success` and `Failure` also carries a **status**: the kind of success or failure it was, not just that it happened.

```ts
import { Outcomes } from "@kiitdev/result";
import type { Outcome } from "@kiitdev/result";

function withdraw(balance: number, amount: number): Outcome<number> {
  if (amount <= 0) return Outcomes.invalid("Amount must be positive");
  if (amount > balance) return Outcomes.rejected("Insufficient funds");
  return Outcomes.success(balance - amount);
}

const result = withdraw(50, 80);
result.status.name;   // "RULE_VIOLATION"
result.success;       // false
```

A `Success` might be a plain success, something pending, or an intentional skip. A `Failure` might be unauthorized, invalid, or a business-rule conflict. The statuses come from [kiit-codes](https://www.npmjs.com/package/@kiitdev/codes), an established taxonomy of eight fixed groups, so the same vocabulary works for logs, HTTP mapping, and error handling instead of every project inventing its own. It's a native TypeScript port of the Kotlin `kiit-result`, checked against it. It isn't a wrapper.

Pre-1.0: the API may still shift before a stable release.

## Install

```bash
npm install @kiitdev/result @kiitdev/codes
```

## Success and Failure

`Success<T>` and `Failure<E>` are two classes, and `Result<T, E>` is their union. Narrow on `success`, and the status narrows with it (`Passed` on a success, `Failed` on a failure).

```ts
import { Success, Failure } from "@kiitdev/result";
import type { Result } from "@kiitdev/result";

function describe(r: Result<number, string>): string {
  return r.success ? `got ${r.value} (${r.status.name})` : `failed: ${r.error} (${r.status.name})`;
}

describe(new Success(42));                              // "got 42 (SUCCESS)"
describe(new Failure("no such user"));                  // "failed: no such user (UNEXPECTED)"
```

The constructors default the status per branch. To pick one, pass it: `new Success(user, Succeeded.CREATED)`, `new Failure(err, Restricted.UNAUTHORIZED)`. A `Success` only accepts a `Passed` status and a `Failure` only a `Failed` one, so mixing them up is a compile error.

The usual operators are methods: `map`, `mapError`, `flatMap` (also `andThen`), `fold`, `or`, `and`, `orElse`, `recover`, `transform`, `getOr`, `getOrElse`, `getOrNull`, `getOrThrow`, `onSuccess`, `onFailure`, `exists`. They keep the status and any attached `Action`. There's no `then` method on purpose: an object with `then` is a thenable, so `await` and async returns would call it as a promise callback. Use `flatMap` or `andThen`.

## Builders

`Outcomes`, `Tries`, `Options` and `Validations` build a `Result` with the right status for you.

| Alias | Same as | Failure holds | Builder |
|---|---|---|---|
| `Outcome<T>` | `Result<T, Err>` | a kiit-codes `Err` | `Outcomes` |
| `Try<T>` | `Result<T, Error>` | an `Error` | `Tries` |
| `Option<T>` | `Result<T, undefined>` | nothing | `Options` |
| `Validated<T>` | `Result<T, ErrorList>` | every validation error | `Validations` |

Each builder has `success`, `pending`, `excluded` and `information`, plus `restricted`, `invalid`, `rejected` and `unserved`. A failure builder takes a message, an options object, or both, and defaults to its group's default status:

```ts
import { Restricted } from "@kiitdev/codes";
import { Outcomes } from "@kiitdev/result";

Outcomes.restricted();                                          // status DENIED
Outcomes.restricted("token expired");                           // message is the error
Outcomes.restricted("token expired", { status: Restricted.UNAUTHORIZED });
Outcomes.restricted({ cause: new Error("boom") });              // from an Error
Outcomes.restricted({ err: someErr });                          // from an Err
```

`Outcomes.attempt(f)` and `Tries.attempt(f)` run a function and turn a throw into a `Failure`. `Validations.of(value, errors)` gives a `Success` when the list is empty and one `Failure` holding all of them otherwise.

## Async

Async work stays a `Promise<Result>`. `attemptAsync` catches a rejection, and `mapAsync`, `flatMapAsync`, `onSuccessAsync` and `onFailureAsync` take an async callback.

```ts
const user = await Outcomes.attemptAsync("fetchUser", () => db.find(id));
const email = await user.mapAsync(async (u) => (await loadProfile(u)).email);
```

A rejection inside a `mapAsync` callback rejects the promise, the same way a throw inside `map` would. Wrap the call in `attemptAsync` to get a `Failure` instead.

## Domain errors with HasStatus

Your own success and error types can carry their status, and `success`, `failure` and `build` wire it into a `Result`.

```ts
import type { Failed, HasStatus } from "@kiitdev/codes";
import { failure } from "@kiitdev/result";

class EmailTaken implements HasStatus<Failed> {
  readonly kind = "EmailTaken";
  readonly status: Failed = EMAIL_TAKEN;   // a custom Rejected code
  constructor(readonly email: string) {}
}

failure(new EmailTaken("a@b.com"));        // Failure, status is EMAIL_TAKEN
```

## Learn more

- [Full docs](https://www.kiit.dev/docs/kiit-result) (Kotlin-focused, same model)
- [kiit-codes](https://www.kiit.dev/docs/kiit-codes): the status taxonomy
- [Sample app](https://github.com/kiitdev/kiit-result/tree/main/samples/sample-ts): every scenario above, runnable

## License

[Apache License 2.0](./LICENSE)
