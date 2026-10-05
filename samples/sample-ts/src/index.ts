/**
 * Living documentation of @kiitdev/result from real TypeScript, type-checked (`npm run typecheck`)
 * against the actual native port. Mirrors the scenarios in samples/sample-kotlin: Outcome
 * validation, Validated, a service that returns Outcomes, and domain errors with HasStatus. Ends
 * with a short async section.
 *
 * Organized into one function per scenario, run in order at the bottom of this file.
 */
import { CodesToHttp, Err, Invalid, Rejected, Restricted, assertNever } from "@kiitdev/codes";
import { Failure, Outcomes, Success, Validations, combine, partition } from "@kiitdev/result";
import type { Outcome, Result, Validated } from "@kiitdev/result";
import { Created, EmailTaken, InvalidEmail, Unauthorized, UserService } from "./user-service.js";
import type { CreateUserError, CreateUserSuccess, User } from "./user-service.js";

const http = CodesToHttp();

function check(condition: boolean, label: string): void {
  if (!condition) {
    throw new Error(`FAILED: ${label}`);
  }
  console.log(`ok: ${label}`);
}

function section(title: string): void {
  console.log(`\n${"=".repeat(60)}\n${title}\n${"=".repeat(60)}`);
}

// Single-error validation with Outcome<T> (Result<T, Err>): picks a different status group
// depending on why the phone number failed.
function validatePhone(phone: string, caller: string = "guest"): Outcome<string> {
  if (phone.length === 0) return Outcomes.invalid({ err: Err.on("phone", phone, "Too short") });
  if (phone.length > 10) return Outcomes.invalid({ err: Err.on("phone", phone, "Too long") });
  if (phone === "1111111111") return Outcomes.rejected({ err: Err.on("phone", phone, "Reserved phone for testing") });
  if (phone.startsWith("123") && caller !== "admin") {
    return Outcomes.restricted({ err: Err.on("phone", phone, "Only admins can validate internal-use numbers") });
  }
  return Outcomes.success(phone);
}

function testOutcome(): void {
  section("Outcome: one error, different status groups");
  const results = [
    validatePhone(""),
    validatePhone("12345678901"),
    validatePhone("1111111111"),
    validatePhone("1234567890"),
    validatePhone("9876543210"),
    validatePhone("1234567890", "admin"),
  ];
  for (const r of results) {
    console.log(`${r.status.name} success=${r.success} ${r.fold((v) => `value=${v}`, (e) => `error=${e.message}`)}`);
  }
  check(results.map((r) => r.status.group).join() === "Invalid,Invalid,Rejected,Restricted,Succeeded,Succeeded", "status groups");

  // Narrowing on `success` also narrows the status: Passed on one branch, Failed on the other.
  const first = results[0]!;
  if (first.success) {
    check(false, "empty phone should have failed");
  } else {
    check(first.status.group === "Invalid" && first.error.message === "Too short", "narrowed failure");
  }
}

interface UserForm {
  readonly name: string;
  readonly email: string;
  readonly phone: string;
}

// Multi-error validation with Validated<T> (Result<T, ErrorList>): checks every field and
// reports them all together in one Failure.
function validateUser(form: UserForm): Validated<UserForm> {
  const errors: Err[] = [];
  if (form.name.trim() === "") errors.push(Err.on("name", form.name, "Name is required"));
  if (!form.email.includes("@")) errors.push(Err.on("email", form.email, "Email must contain @"));
  if (form.phone.length !== 10) errors.push(Err.on("phone", form.phone, "Phone must be 10 digits"));
  return Validations.of(form, errors);
}

function testValidation(): void {
  section("Validated: report every error at once");
  const valid = validateUser({ name: "Alice", email: "alice@example.com", phone: "1234567890" });
  const invalid = validateUser({ name: "", email: "not-an-email", phone: "123" });

  valid.onSuccess((form) => console.log("valid form:", form));
  invalid.onFailure((list) => {
    console.log(`invalid form, ${list.errors.length} error(s):`);
    for (const e of list.errors) console.log(`  - ${e.message}`);
  });
  check(valid.success, "valid form is a success");
  check(invalid.getErrorOrNull()?.errors.length === 3, "invalid form reports all 3 errors");
  check(invalid.status.name === Invalid.INVALID_VALUE.name, "validation failure uses INVALID_VALUE");
}

function report(label: string, outcome: Outcome<User>): void {
  const status = outcome.status;
  const detail = outcome.fold(
    (user) => `user=${user.id}`,
    (err) => `error=${err.message}`,
  );
  console.log(`${label} -> ${status.name} (success=${outcome.success}, http=${http.toCode(status)}, ${detail})`);
}

function testService(): void {
  section("Service: an Outcome for every operation");
  const service = new UserService();

  const created = service.create("alice", "alice@example.com");
  report("create alice", created);
  report("create alice again", service.create("alice", "alice@example.com"));
  report("create with blank email", service.create("bob", ""));
  report("authorize alice as alice", service.authorize("alice", "alice"));
  report("authorize alice as bob", service.authorize("alice", "bob"));
  report("authorize unknown user", service.authorize("carol", "carol"));

  check(created.success, "create succeeds");
  check(service.create("alice", "x@y.z").status.name === Rejected.CONFLICT.name, "duplicate is a CONFLICT");
  check(service.authorize("alice", "bob").status.name === Restricted.UNAUTHORIZED.name, "wrong requester is UNAUTHORIZED");
  check(http.toCode(service.fetch("nobody").status) === 404, "unknown user maps to HTTP 404");

  // Composition: map / onSuccess / onFailure chaining.
  service
    .fetch("alice")
    .map((user) => user.email)
    .onSuccess((email) => console.log("alice's email:", email))
    .onFailure((err) => console.log("could not fetch alice:", err.message));

  // toTry() converts a Failure<Err> into a Failure<Error> (a StatusError), so it can cross a
  // call boundary that only communicates through exceptions.
  service.fetch("missing").toTry().onFailure((error) => console.log("caught as exception:", error.message));
}

function reportTyped(label: string, result: Result<CreateUserSuccess, CreateUserError>): void {
  const detail = result.fold(
    (created) => `user=${created.user.id}`,
    (error) => `error=${describeError(error)}`,
  );
  console.log(`${label} -> ${result.status.name} (success=${result.success}, http=${http.toCode(result.status)}, ${detail})`);
}

// A switch over the error's `kind`, with assertNever so a new variant fails to compile here.
function describeError(error: CreateUserError): string {
  switch (error.kind) {
    case "EmailTaken":
      return `email taken: ${error.email}`;
    case "InvalidEmail":
      return `invalid email: ${error.email}`;
    case "Unauthorized":
      return "unauthorized";
    default:
      return assertNever(error);
  }
}

function testDomainErrors(): void {
  section("Domain errors: Result<CreateUserSuccess, CreateUserError>");
  const service = new UserService();

  const first = service.createTyped("alice", "alice@example.com", true);
  const again = service.createTyped("alice", "alice@example.com", true);
  reportTyped("create alice", first);
  reportTyped("create alice again", again);
  reportTyped("create with invalid email", service.createTyped("bob", "not-an-email", true));
  reportTyped("create without authorization", service.createTyped("carol", "carol@example.com", false));

  check(first.getOrNull() instanceof Created, "success carries the domain value");
  check(again.getErrorOrNull() instanceof EmailTaken, "duplicate is EmailTaken");
  check(service.createTyped("x", "bad", true).getErrorOrNull() instanceof InvalidEmail, "bad email is InvalidEmail");
  check(service.createTyped("x", "x@y.z", false).getErrorOrNull() instanceof Unauthorized, "no auth is Unauthorized");
}

async function testAsync(): Promise<void> {
  section("Async: attemptAsync, mapAsync, flatMapAsync");
  const service = new UserService();
  service.create("alice", "alice@example.com");

  // attemptAsync catches a rejection and gives an Outcome with the status UNEXPECTED.
  const found = await service.fetchAsync("alice");
  const missing = await service.fetchAsync("nobody");
  console.log("fetchAsync alice:", found.fold((u) => u.id, (e) => e.message));
  console.log("fetchAsync nobody:", missing.status.name, missing.action?.action);

  // mapAsync / flatMapAsync take an async callback. A Failure skips it.
  const email = await found.mapAsync(async (user) => user.email.toUpperCase());
  const skipped = await missing.mapAsync(async (user) => user.email.toUpperCase());
  check(email.getOrNull() === "ALICE@EXAMPLE.COM", "mapAsync runs on a success");
  const chained = await found.flatMapAsync(async (user) => service.fetchAsync(user.id));
  check(chained.getOrNull()?.id === "alice", "flatMapAsync chains another async Outcome");
  check(skipped.success === false, "mapAsync skips a failure");
  check(missing.action?.action === "fetch:nobody", "attemptAsync tagged the action");
}

function testLists(): void {
  section("Lists: combine and partition");
  const ok = [Outcomes.success(1), Outcomes.success(2)];
  const mixed = [Outcomes.success(1), Outcomes.invalid("bad"), Outcomes.success(3)];

  check(combine(ok).getOrNull()?.join() === "1,2", "combine gathers every value");
  check(combine(mixed).success === false, "combine stops at the first failure");
  const [values, errors] = partition(mixed);
  check(values.join() === "1,3" && errors.length === 1, "partition splits values and errors");

  // Plain Success and Failure are fine when the error isn't an Err.
  const parse = (text: string): Result<number, string> =>
    Number.isNaN(Number(text)) ? new Failure(`not a number: ${text}`) : new Success(Number(text));
  check(parse("42").getOrNull() === 42 && parse("x").getErrorOrNull() === "not a number: x", "any error type works");
}

testOutcome();
testValidation();
testService();
testDomainErrors();
testLists();
await testAsync();
console.log("\nAll checks passed.");
