/** /api/chat streams newline-delimited JSON, one of these per line. */
export type StreamEvent =
  | { type: "text"; text: string }
  | { type: "tool"; id: number; status: "start" | "done" | "error" | "denied"; label: string }
  /** The reply is paused until the user allows or denies this call via /api/chat/approve. */
  | { type: "approval"; id: number; runId: string; label: string; detail: string }
  | { type: "error"; message: string };
