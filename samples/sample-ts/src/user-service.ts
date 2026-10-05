import { Invalid, Rejected, Restricted, Succeeded } from "@kiitdev/codes";
import type { Failed, HasStatus, Passed } from "@kiitdev/codes";
import { Outcomes, failure, success } from "@kiitdev/result";
import type { Outcome, Result } from "@kiitdev/result";

export interface User {
  readonly id: string;
  readonly email: string;
}

// Custom domain codes for createTyped below, each paired with its kiit-codes group.
const USER_CREATED = Succeeded("USER_CREATED", "User account created", "sample");
const EMAIL_TAKEN = Rejected("EMAIL_TAKEN", "Email is already registered", "sample");
const INVALID_EMAIL = Invalid("INVALID_EMAIL", "Email format is invalid", "sample");
const UNAUTHORIZED_CREATE = Restricted("UNAUTHORIZED_CREATE", "Not authorized to create users", "sample");

// Every way createTyped can succeed. Each variant carries its own status.
export class Created implements HasStatus<Passed> {
  readonly kind = "Created";
  readonly status: Passed = USER_CREATED;
  constructor(readonly user: User) {}
}
export type CreateUserSuccess = Created;

// Every way createTyped can fail.
export class EmailTaken implements HasStatus<Failed> {
  readonly kind = "EmailTaken";
  readonly status: Failed = EMAIL_TAKEN;
  constructor(readonly email: string) {}
}
export class InvalidEmail implements HasStatus<Failed> {
  readonly kind = "InvalidEmail";
  readonly status: Failed = INVALID_EMAIL;
  constructor(readonly email: string) {}
}
export class Unauthorized implements HasStatus<Failed> {
  readonly kind = "Unauthorized";
  readonly status: Failed = UNAUTHORIZED_CREATE;
}
export type CreateUserError = EmailTaken | InvalidEmail | Unauthorized;

/**
 * A tiny service that returns an Outcome (Result<T, Err>) for every operation instead of
 * throwing for expected failures. Status groups come from kiit-codes.
 */
export class UserService {
  private readonly users = new Map<string, User>();

  create(id: string, email: string): Outcome<User> {
    if (email.trim() === "") return Outcomes.invalid({ status: Invalid.BAD_REQUEST });
    if (this.users.has(id)) return Outcomes.rejected({ status: Rejected.CONFLICT });
    const user = { id, email };
    this.users.set(id, user);
    return Outcomes.success(user);
  }

  fetch(id: string): Outcome<User> {
    const user = this.users.get(id);
    return user ? Outcomes.success(user) : Outcomes.invalid({ status: Invalid.NOT_FOUND });
  }

  authorize(id: string, requesterId: string): Outcome<User> {
    return this.fetch(id).flatMap((user) =>
      user.id !== requesterId ? Outcomes.restricted({ status: Restricted.UNAUTHORIZED }) : Outcomes.success(user),
    );
  }

  // Same operation as create, but with a domain-specific Result<CreateUserSuccess, CreateUserError>
  // instead of Outcome<User>. The signature lists every value and error that's possible.
  createTyped(id: string, email: string, isAuthorized: boolean): Result<CreateUserSuccess, CreateUserError> {
    if (!isAuthorized) return failure(new Unauthorized());
    if (!email.includes("@")) return failure(new InvalidEmail(email));
    if (this.users.has(id)) return failure(new EmailTaken(email));
    const user = { id, email };
    this.users.set(id, user);
    return success(new Created(user));
  }

  /** Same as fetch, but async, the way a real service would call a database. */
  async fetchAsync(id: string): Promise<Outcome<User>> {
    return Outcomes.attemptAsync(`fetch:${id}`, async () => {
      await Promise.resolve();
      return this.fetch(id).getOrThrow();
    });
  }
}
