import { describe, expect, it } from "vitest";
import { Invalid, Rejected, Restricted, Succeeded, Pending, Unserved } from "@kiitdev/codes";
import type { Failed, HasStatus, Passed, Status } from "@kiitdev/codes";
import { build, failure, success } from "../src/index.js";
import type { Result } from "../src/index.js";

// Ported from HasStatusTests.kt. Kotlin's sealed classes become tagged unions of small classes.

const USER_CREATED = Succeeded("USER_CREATED", "User account created", "users");
const QUEUED_FOR_REVIEW = Pending("QUEUED_FOR_REVIEW", "Flagged for manual review before activation", "users");
const EMAIL_TAKEN = Rejected("EMAIL_TAKEN", "Email is already registered", "users");
const INVALID_EMAIL = Invalid("INVALID_EMAIL", "Email format is invalid", "users");
const UNAUTHORIZED_CREATE = Restricted("UNAUTHORIZED_CREATE", "Not authorized to create users", "users");

class Created implements HasStatus<Passed> {
  readonly kind = "Created";
  readonly status: Passed = USER_CREATED;
  constructor(readonly email: string) {}
}
class QueuedForReview implements HasStatus<Passed> {
  readonly kind = "QueuedForReview";
  readonly status: Passed = QUEUED_FOR_REVIEW;
  constructor(readonly email: string) {}
}
type CreateUserSuccess = Created | QueuedForReview;

class EmailTaken implements HasStatus<Failed> {
  readonly kind = "EmailTaken";
  readonly status: Failed = EMAIL_TAKEN;
  constructor(readonly email: string) {}
}
class InvalidEmail implements HasStatus<Failed> {
  readonly kind = "InvalidEmail";
  readonly status: Failed = INVALID_EMAIL;
  constructor(readonly email: string) {}
}
class Unauthorized implements HasStatus<Failed> {
  readonly kind = "Unauthorized";
  readonly status: Failed = UNAUTHORIZED_CREATE;
}
class DatabaseUnavailable implements HasStatus<Failed> {
  readonly kind = "DatabaseUnavailable";
  readonly status: Failed = Unserved.UNEXPECTED;
  constructor(readonly cause: Error) {}
}
type CreateUserError = EmailTaken | InvalidEmail | Unauthorized | DatabaseUnavailable;

function createUser(
  email: string,
  isAuthorized: boolean,
  existingEmails: ReadonlySet<string>,
  needsReview: boolean,
): Result<CreateUserSuccess, CreateUserError> {
  if (!isAuthorized) return failure(new Unauthorized());
  if (!email.includes("@")) return failure(new InvalidEmail(email));
  if (existingEmails.has(email)) return failure(new EmailTaken(email));
  if (needsReview) return success(new QueuedForReview(email));
  return success(new Created(email));
}

describe("HasStatus success and failure helpers", () => {
  it("each domain success variant carries its own custom status", () => {
    expect(new Created("a@b.com").status).toEqual(USER_CREATED);
    expect(new QueuedForReview("a@b.com").status).toEqual(QUEUED_FOR_REVIEW);
  });

  it("each domain error variant carries its own custom status", () => {
    expect(new Unauthorized().status).toEqual(UNAUTHORIZED_CREATE);
    expect(new EmailTaken("a@b.com").status).toEqual(EMAIL_TAKEN);
    expect(new InvalidEmail("bad").status).toEqual(INVALID_EMAIL);
    expect(new DatabaseUnavailable(new Error()).status).toEqual(Unserved.UNEXPECTED);
  });

  it("success wires the domain outcome's own status into the result", () => {
    const result = createUser("a@b.com", true, new Set(), true);
    expect(result.success).toBe(true);
    expect(result.status).toEqual(QUEUED_FOR_REVIEW);
    expect(result.getOrNull()).toEqual(new QueuedForReview("a@b.com"));
  });

  it("failure wires the domain error's own status into the result", () => {
    const result = createUser("bad", true, new Set(), false);
    expect(result.success).toBe(false);
    expect(result.status).toEqual(INVALID_EMAIL);
    expect(result.getErrorOrNull()).toEqual(new InvalidEmail("bad"));
  });

  it("reports unauthorized before the other checks", () => {
    expect(createUser("bad", false, new Set(), false).status).toEqual(UNAUTHORIZED_CREATE);
  });

  it("reports email taken when authorized and well-formed", () => {
    expect(createUser("a@b.com", true, new Set(["a@b.com"]), false).status).toEqual(EMAIL_TAKEN);
  });

  it("creates directly when nothing else applies", () => {
    const result = createUser("a@b.com", true, new Set(), false);
    expect(result.success).toBe(true);
    expect(result.status).toEqual(USER_CREATED);
    expect(result.getOrNull()).toEqual(new Created("a@b.com"));
  });
});

const ORDER_PLACED = Succeeded("ORDER_PLACED", "Order placed", "orders");
const ORDER_OUT_OF_STOCK = Rejected("ORDER_OUT_OF_STOCK", "Item is out of stock", "orders");

class Placed implements HasStatus<Status> {
  readonly status: Status = ORDER_PLACED;
  constructor(readonly orderId: string) {}
}
class OutOfStock implements HasStatus<Status> {
  readonly status: Status = ORDER_OUT_OF_STOCK;
  constructor(readonly sku: string) {}
}
type PlaceOrderResult = Placed | OutOfStock;

function placeOrder(sku: string, inStock: boolean): Result<PlaceOrderResult, PlaceOrderResult> {
  return build<PlaceOrderResult>(inStock ? new Placed("ord-1") : new OutOfStock(sku));
}

describe("HasStatus build helper", () => {
  it("infers a success from a passed status", () => {
    const result = placeOrder("sku-1", true);
    expect(result.success).toBe(true);
    expect(result.status).toEqual(ORDER_PLACED);
    expect(result.getOrNull()).toEqual(new Placed("ord-1"));
  });

  it("infers a failure from a failed status", () => {
    const result = placeOrder("sku-1", false);
    expect(result.success).toBe(false);
    expect(result.status).toEqual(ORDER_OUT_OF_STOCK);
    expect(result.getErrorOrNull()).toEqual(new OutOfStock("sku-1"));
  });
});
