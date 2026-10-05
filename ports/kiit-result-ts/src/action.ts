/**
 * Describes the operation that produced or wrapped a `Result`, e.g. `Action("chargeCard", { xid: "req-1" })`.
 * `previous` links to the action that was on the result before, so nested operations keep their history.
 *
 * Like Status and Err in kiit-codes, this is a plain object so it survives `JSON.stringify` /
 * `JSON.parse` unchanged.
 */
export interface Action {
  readonly action: string;
  /** External/correlation id, e.g. a request id. */
  readonly xid?: string;
  readonly data: Readonly<Record<string, string>>;
  readonly previous?: Action;
}

export interface ActionOptions {
  readonly xid?: string;
  readonly data?: Readonly<Record<string, string>>;
  readonly previous?: Action;
}

export function Action(action: string, options: ActionOptions = {}): Action {
  return { action, xid: options.xid, data: options.data ?? {}, previous: options.previous };
}
