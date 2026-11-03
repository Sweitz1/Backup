import { auth } from "@/auth";
import { listModels, DEFAULT_MODEL } from "@/lib/llm/ollama";

export const runtime = "nodejs";

export async function GET() {
  if (!(await auth())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  try {
    return Response.json({ models: await listModels(), default: DEFAULT_MODEL });
  } catch (err) {
    return Response.json(
      { error: `Model server unavailable: ${(err as Error).message}`, models: [], default: DEFAULT_MODEL },
      { status: 502 },
    );
  }
}
