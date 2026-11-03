/** /api/chat streams newline-delimited JSON, one of these per line. */
export type StreamEvent =
  | { type: "text"; text: string }
  | { type: "tool"; id: number; status: "start"; label: string }
  | { type: "tool"; id: number; status: "done" | "error"; label: string }
  | { type: "error"; message: string };
