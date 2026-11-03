import fs from "node:fs/promises";
import path from "node:path";
import matter from "gray-matter";
import { isKnownTool } from "@/lib/tools/registry";
import { SkillMeta, type Skill } from "./types";

export function skillsDir(): string {
  return path.resolve(process.env.SKILLS_DIR ?? path.join(process.cwd(), "skills"));
}

/**
 * Reads every skills/<folder>/SKILL.md. Invalid skills are skipped with a warning
 * so one bad file can't take the app down.
 */
export async function loadSkills(dir = skillsDir()): Promise<Skill[]> {
  let entries: import("node:fs").Dirent[];
  try {
    entries = await fs.readdir(dir, { withFileTypes: true });
  } catch {
    return [];
  }

  const skills: Skill[] = [];
  for (const entry of entries) {
    if (!entry.isDirectory()) continue;
    const file = path.join(dir, entry.name, "SKILL.md");
    try {
      const { data, content } = matter(await fs.readFile(file, "utf8"));
      const meta = SkillMeta.parse(data);
      if (meta.name !== entry.name) {
        throw new Error(`name "${meta.name}" must match folder "${entry.name}"`);
      }
      const unknown = (meta.tools ?? []).filter((t) => !isKnownTool(t));
      if (unknown.length) throw new Error(`unknown tool(s): ${unknown.join(", ")}`);
      skills.push({ ...meta, instructions: content.trim() });
    } catch (err) {
      console.warn(`[skills] skipping ${file}: ${(err as Error).message}`);
    }
  }
  return skills.sort((a, b) => a.name.localeCompare(b.name));
}

export async function getSkill(name: string, dir = skillsDir()): Promise<Skill | undefined> {
  return (await loadSkills(dir)).find((s) => s.name === name);
}
