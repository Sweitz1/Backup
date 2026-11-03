import type { ChatMessage } from "@/lib/llm/ollama";
import type { Skill } from "./types";

/** Prepends the active skill's instructions as the system message. */
export function buildMessages(history: ChatMessage[], skill?: Skill): ChatMessage[] {
  const convo = history.filter((m) => m.role !== "system");
  return skill ? [{ role: "system", content: skill.instructions }, ...convo] : convo;
}
