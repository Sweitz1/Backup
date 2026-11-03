import { Ollama } from "ollama";

export const ollama = new Ollama({
  host: process.env.OLLAMA_HOST ?? "http://127.0.0.1:11434",
});

export const DEFAULT_MODEL = process.env.DEFAULT_MODEL ?? "llama3.1";

export type ChatMessage = { role: "system" | "user" | "assistant"; content: string };

/** Names of models already pulled into the local Ollama server. */
export async function listModels(): Promise<string[]> {
  const { models } = await ollama.list();
  return models.map((m) => m.name);
}
