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

/** A validated tool call, ready to approve and run. */
export type PreparedCall = {
  ok: true;
  tool: ToolDef<any>;
  args: unknown;
  label: string;
  /** What will be sent off the machine, if anything. */
  detail: string;
  external: boolean;
};

/**
 * Checks a tool call from the model: the tool must have been offered and the arguments
 * must validate. Problems come back as a ToolResult so they can be returned to the model.
 */
export function prepareTool(available: ToolDef<any>[], name: string, rawArgs: unknown): PreparedCall | ToolResult {
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
  return {
    ok: true,
    tool,
    args: parsed.data,
    label,
    detail: tool.detail?.(parsed.data) ?? label,
    external: tool.external === true,
  };
}

export function isPrepared(p: PreparedCall | ToolResult): p is PreparedCall {
  return p.ok && "tool" in p;
}

/** Runs a prepared call. Never throws; errors go back to the model as text. */
export async function executeTool(call: PreparedCall): Promise<ToolResult> {
  try {
    const out = await call.tool.run(call.args);
    return {
      ok: true,
      label: call.label,
      output: out.length > MAX_OUTPUT_CHARS ? out.slice(0, MAX_OUTPUT_CHARS) + "\n[output truncated]" : out,
    };
  } catch (err) {
    return { ok: false, label: call.label, output: `Error: ${(err as Error).message}` };
  }
}

/** Validate and run in one step (no approval). */
export async function runTool(available: ToolDef<any>[], name: string, rawArgs: unknown): Promise<ToolResult> {
  const prepared = prepareTool(available, name, rawArgs);
  return isPrepared(prepared) ? executeTool(prepared) : prepared;
}
