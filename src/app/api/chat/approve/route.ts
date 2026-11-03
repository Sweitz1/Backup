import { z } from "zod";
import { auth } from "@/auth";
import { decide } from "@/lib/tools/approvals";

export const runtime = "nodejs";

const Body = z.object({
  runId: z.string().uuid(),
  id: z.number().int().min(0),
  allow: z.boolean(),
});

/** The user's Allow / Deny for a paused tool call. Only the user who started the reply can decide. */
export async function POST(req: Request) {
  const session = await auth();
  if (!session) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const parsed = Body.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return Response.json({ error: "Invalid request" }, { status: 400 });

  const { runId, id, allow } = parsed.data;
  const found = decide(session.user?.email ?? "unknown", runId, id, allow);
  if (!found) return Response.json({ error: "No such pending request (it may have timed out)" }, { status: 404 });
  return Response.json({ ok: true });
}
