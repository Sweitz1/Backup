import { z } from "zod";
import { auth } from "@/auth";
import { ollama, DEFAULT_MODEL } from "@/lib/llm/ollama";
import { getSkill } from "@/lib/skills/loader";
import { buildMessages } from "@/lib/skills/prompt";

export const runtime = "nodejs";

const Body = z.object({
  messages: z
    .array(
      z.object({
        role: z.enum(["user", "assistant"]), // clients can't inject system prompts
        content: z.string().max(100_000),
      }),
    )
    .min(1)
    .max(200),
  model: z.string().max(200).optional(),
  skill: z.string().max(100).optional(),
});

/** Streams the model's reply as plain text. Nothing is logged or stored. */
export async function POST(req: Request) {
  if (!(await auth())) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const parsed = Body.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return Response.json({ error: "Invalid request" }, { status: 400 });

  const { messages, model, skill: skillName } = parsed.data;
  const skill = skillName ? await getSkill(skillName) : undefined;
  if (skillName && !skill) return Response.json({ error: "Unknown skill" }, { status: 400 });

  let response;
  try {
    response = await ollama.chat({
      model: skill?.model ?? model ?? DEFAULT_MODEL,
      messages: buildMessages(messages, skill),
      stream: true,
      options: skill?.temperature !== undefined ? { temperature: skill.temperature } : undefined,
    });
  } catch (err) {
    return Response.json(
      { error: `Model server unavailable: ${(err as Error).message}` },
      { status: 502 },
    );
  }

  const encoder = new TextEncoder();
  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      try {
        for await (const part of response) {
          controller.enqueue(encoder.encode(part.message.content));
        }
        controller.close();
      } catch (err) {
        controller.error(err);
      }
    },
    cancel() {
      response.abort(); // stop generation when the user hits Stop or closes the tab
    },
  });

  return new Response(stream, {
    headers: { "Content-Type": "text/plain; charset=utf-8", "Cache-Control": "no-store" },
  });
}
