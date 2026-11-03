import { z } from "zod";

/** Frontmatter at the top of skills/<name>/SKILL.md */
export const SkillMeta = z.object({
  name: z.string().regex(/^[a-z0-9-]+$/, "lowercase letters, digits and dashes only"),
  description: z.string().min(1),
  model: z.string().optional(), // overrides the chosen model when this skill is active
  temperature: z.number().min(0).max(2).optional(),
  tools: z.array(z.string()).optional(), // tool names from src/lib/tools/registry.ts
});

export type SkillMeta = z.infer<typeof SkillMeta>;

export type Skill = SkillMeta & {
  /** Markdown body of SKILL.md, used as the system prompt. */
  instructions: string;
};

/** What the browser is allowed to see (no instructions). */
export type SkillSummary = Pick<Skill, "name" | "description" | "tools">;
