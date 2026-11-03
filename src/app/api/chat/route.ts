import { z } from "zod";
import type { Message, ToolCall } from "ollama";
import { auth } from "@/auth";
import { ollama, DEFAULT_MODEL } from "@/lib/llm/ollama";
import type { StreamEvent } from "@/lib/llm/events";
import { getSkill } from "@/lib/skills/loader";
import { buildMessages } from "@/lib/skills/prompt";
import { cancelRun, waitForApproval } from "@/lib/tools/approvals";
import { executeTool, isPrepared, prepareTool, toOllamaTools, toolsFor } from "@/lib/tools/registry";

export const runtime = "nodejs";

/** Model ↔ tool round trips allowed per reply, so a confused model can't loop forever. */
const MAX_TOOL_ROUNDS = 6;

const Body = z.object({
  messages: z
    .array(
      z.object({
        role: z.enum(["user", "assistant"]), // clients can't inject system prompts or tool results
        content: z.string().max(100_000),
      }),
    )
    .min(1)
    .max(200),
  model: z.string().max(200).optional(),
  skill: z.string().max(100).optional(),
  /** Ask the user before any tool sends data off this machine. */
  confirmExternal: z.boolean().default(true),
});

/**
 * Streams the reply as newline-delimited JSON (see StreamEvent). When the active skill
 * has tools, runs the model ↔ tool loop on the server. Nothing is logged or stored.
 */
export async function POST(req: Request) {
  const session = await auth();
  if (!session) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const owner = session.user?.email ?? "unknown";

  const parsed = Body.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return Response.json({ error: "Invalid request" }, { status: 400 });

  const { messages, model, skill: skillName, confirmExternal } = parsed.data;
  const runId = crypto.randomUUID();
  const skill = skillName ? await getSkill(skillName) : undefined;
  if (skillName && !skill) return Response.json({ error: "Unknown skill" }, { status: 400 });

  const tools = toolsFor(skill?.tools);
  const ollamaTools = tools.length ? toOllamaTools(tools) : undefined;
  const convo: Message[] = buildMessages(messages, skill, tools.length > 0);
  const options = skill?.temperature !== undefined ? { temperature: skill.temperature } : undefined;
  const chosenModel = skill?.model ?? model ?? DEFAULT_MODEL;

  const encoder = new TextEncoder();
  let current: { abort(): void } | undefined;
  let cancelled = false;

  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      const send = (event: StreamEvent) => {
        if (!cancelled) controller.enqueue(encoder.encode(JSON.stringify(event) + "\n"));
      };
      let toolId = 0;

      try {
        for (let round = 0; ; round++) {
          const response = await ollama.chat({
            model: chosenModel,
            messages: convo,
            stream: true,
            tools: ollamaTools,
            options,
          });
          current = response;

          let content = "";
          const calls: ToolCall[] = [];
          for await (const part of response) {
            if (part.message.content) {
              content += part.message.content;
              send({ type: "text", text: part.message.content });
            }
            if (part.message.tool_calls?.length) calls.push(...part.message.tool_calls);
          }

          if (cancelled || calls.length === 0) break;
          if (round >= MAX_TOOL_ROUNDS) {
            send({ type: "error", message: "Stopped: too many tool calls in one reply." });
            break;
          }

          convo.push({ role: "assistant", content, tool_calls: calls });
          for (const call of calls) {
            const id = toolId++;
            const name = call.function.name;
            const reply = (content: string) => convo.push({ role: "tool", content, tool_name: name } as Message);

            const prepared = prepareTool(tools, name, call.function.arguments);
            if (!isPrepared(prepared)) {
              send({ type: "tool", id, status: "error", label: prepared.label });
              reply(prepared.output);
              continue;
            }

            if (prepared.external && confirmExternal) {
              send({ type: "approval", id, runId, label: prepared.label, detail: prepared.detail });
              const allowed = await waitForApproval(owner, runId, id);
              if (cancelled) break;
              if (!allowed) {
                send({ type: "tool", id, status: "denied", label: prepared.label });
                reply("The user declined this request. Don't retry it; continue without it or ask the user how to proceed.");
                continue;
              }
            }

            send({ type: "tool", id, status: "start", label: prepared.label });
            const result = await executeTool(prepared);
            if (cancelled) break;
            send({ type: "tool", id, status: result.ok ? "done" : "error", label: result.label });
            reply(result.output);
          }
          if (content) send({ type: "text", text: "\n\n" });
        }
      } catch (err) {
        send({ type: "error", message: `Model server error: ${(err as Error).message}` });
      }
      cancelRun(runId); // nothing left waiting once the reply ends
      if (!cancelled) controller.close();
    },
    cancel() {
      cancelled = true;
      cancelRun(runId);
      current?.abort(); // stop generation when the user hits Stop or closes the tab
    },
  });

  return new Response(stream, {
    headers: { "Content-Type": "application/x-ndjson; charset=utf-8", "Cache-Control": "no-store" },
  });
}
