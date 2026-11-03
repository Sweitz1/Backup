import { z } from "zod";
import type { Tool as OllamaTool } from "ollama";
import { calculator } from "./calculator";
import { listFiles, readFile } from "./files";
import { webFetch } from "./web-fetch";
import { webSearch } from "./web-search";
import type { ToolDef, ToolResult } from "./types";

// Register new tools here.
const ALL = [webSearch, webFetch, listFiles, readFile, calculator] as ToolDef<any>[];

const MAX_OUTPUT_CHARS = 20_000;

export const TOOL_NAMES = ALL.map((t) => t.name);

export function isKnownTool(name: string): boolean {
  return TOOL_NAMES.includes(name);
}

/** Tools a skill asked for that are also switched on. */
export function toolsFor(names: string[] = []): ToolDef<any>[] {
  return ALL.filter((t) => names.includes(t.name) && (t.enabled?.() ?? true));
}

/** Converts tools to the format Ollama sends to the model. */
export function toOllamaTools(tools: ToolDef<any>[]): OllamaTool[] {
  return tools.map((t) => {
    const { $schema: _ignored, ...parameters } = z.toJSONSchema(t.schema, { io: "input" }) as Record<string, unknown>;
    return {
      type: "function",
      function: { name: t.name, description: t.description, parameters },
    } as OllamaTool;
  });
}

/**
 * Runs one tool call from the model. Never throws: errors go back to the model as text
 * so it can correct itself. Only tools offered to the model can run.
 */
export async function runTool(available: ToolDef<any>[], name: string, rawArgs: unknown): Promise<ToolResult> {
  const tool = available.find((t) => t.name === name);
  if (!tool) return { ok: false, label: `Unknown tool ${name}`, output: `Error: tool "${name}" is not available.` };

  const parsed = tool.schema.safeParse(rawArgs ?? {});
  if (!parsed.success) {
    return {
      ok: false,
      label: `${name}: bad arguments`,
      output: `Error: invalid arguments for ${name}: ${parsed.error.issues.map((i) => `${i.path.join(".") || "args"} ${i.message}`).join("; ")}`,
    };
  }

  const label = tool.describe(parsed.data);
  try {
    const out = await tool.run(parsed.data);
    return {
      ok: true,
      label,
      output: out.length > MAX_OUTPUT_CHARS ? out.slice(0, MAX_OUTPUT_CHARS) + "\n[output truncated]" : out,
    };
  } catch (err) {
    return { ok: false, label, output: `Error: ${(err as Error).message}` };
  }
}
