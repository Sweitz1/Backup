/**
 * Holds tool calls that are waiting for the user to click Allow or Deny.
 *
 * In-memory and per-process, which suits a self-hosted single server. Each pending call is
 * tied to the signed-in user who started the reply, so nobody else can approve it.
 */

type Pending = { owner: string; resolve: (allowed: boolean) => void; timer: ReturnType<typeof setTimeout> };

// Kept on globalThis so dev-mode hot reloads don't split it across module copies.
const store = globalThis as typeof globalThis & { __toolApprovals?: Map<string, Pending> };
const pending = (store.__toolApprovals ??= new Map<string, Pending>());

export const APPROVAL_TIMEOUT_MS = 2 * 60_000;

const key = (runId: string, callId: number) => `${runId}:${callId}`;

/** Resolves true if the user allows the call, false if they deny it, it times out, or the reply is stopped. */
export function waitForApproval(
  owner: string,
  runId: string,
  callId: number,
  timeoutMs = APPROVAL_TIMEOUT_MS,
): Promise<boolean> {
  return new Promise((resolve) => {
    const k = key(runId, callId);
    const finish = (allowed: boolean) => {
      const p = pending.get(k);
      if (!p) return;
      clearTimeout(p.timer);
      pending.delete(k);
      resolve(allowed);
    };
    pending.set(k, { owner, resolve: finish, timer: setTimeout(() => finish(false), timeoutMs) });
  });
}

/** Records the user's decision. Returns false if there's no such pending call for this user. */
export function decide(owner: string, runId: string, callId: number, allowed: boolean): boolean {
  const p = pending.get(key(runId, callId));
  if (!p || p.owner !== owner) return false;
  p.resolve(allowed);
  return true;
}

/** Denies everything still pending for a reply (used when the user hits Stop). */
export function cancelRun(runId: string): void {
  for (const [k, p] of pending) {
    if (k.startsWith(`${runId}:`)) p.resolve(false);
  }
}

export function pendingCount(): number {
  return pending.size;
}
