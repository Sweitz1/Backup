import { auth } from "@/auth";
import { loadSkills } from "@/lib/skills/loader";
import type { SkillSummary } from "@/lib/skills/types";

export const runtime = "nodejs";

export async function GET() {
  if (!(await auth())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const skills: SkillSummary[] = (await loadSkills()).map(({ name, description, tools }) => ({
    name,
    description,
    tools,
  }));
  return Response.json({ skills });
}
