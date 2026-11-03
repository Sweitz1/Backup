import type { z } from "zod";

/** A function the model can call while a skill is active. */
export interface ToolDef<A = unknown> {
  /** Name the model calls it by; also what skills list in `tools:`. */
  name: string;
  /** Tells the model when to use it. */
  description: string;
  /** Validates the model's arguments; also converted to JSON Schema for the model. */
  schema: z.ZodType<A>;
  /** Off unless this returns true (e.g. gated by an env var). Default: enabled. */
  enabled?: () => boolean;
  /** Short label shown in the chat UI, e.g. "Fetching example.com". */
  describe: (args: A) => string;
  /**
   * True if the tool sends data off this machine. Such calls can require the user's
   * approval in the chat before they run.
   */
  external?: boolean;
  /** Exactly what will be sent, shown when asking for approval (full URL, search query…). */
  detail?: (args: A) => string;
  /** Returns text for the model. Throw to report an error back to it. */
  run: (args: A) => Promise<string>;
}

export type ToolResult = { ok: boolean; label: string; output: string };
