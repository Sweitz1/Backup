import { describe, expect, it } from "vitest";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { loadSkills, getSkill } from "@/lib/skills/loader";
import { buildMessages } from "@/lib/skills/prompt";

async function tempSkills(files: Record<string, string>) {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), "skills-"));
  for (const [folder, body] of Object.entries(files)) {
    await fs.mkdir(path.join(dir, folder));
    await fs.writeFile(path.join(dir, folder, "SKILL.md"), body);
  }
  return dir;
}

describe("skills loader", () => {
  it("loads the bundled skills", async () => {
    const names = (await loadSkills(path.resolve("skills"))).map((s) => s.name);
    expect(names).toEqual(["code-helper", "files-assistant", "researcher", "summarize"]);
  });

  it("skips skills that ask for unknown tools", async () => {
    const dir = await tempSkills({
      bad: "---\nname: bad\ndescription: x\ntools: [rm_rf]\n---\nx",
      ok: "---\nname: ok\ndescription: x\ntools: [calculator]\n---\nx",
    });
    const skills = await loadSkills(dir);
    expect(skills.map((s) => s.name)).toEqual(["ok"]);
    expect(skills[0].tools).toEqual(["calculator"]);
  });

  it("skips invalid or mismatched skills", async () => {
    const dir = await tempSkills({
      good: "---\nname: good\ndescription: ok\n---\nDo good.",
      "no-desc": "---\nname: no-desc\n---\nMissing description.",
      mismatch: "---\nname: other\ndescription: wrong folder\n---\nx",
    });
    const skills = await loadSkills(dir);
    expect(skills.map((s) => s.name)).toEqual(["good"]);
    expect(skills[0].instructions).toBe("Do good.");
  });

  it("returns nothing for a missing directory", async () => {
    expect(await loadSkills("/nonexistent/skills")).toEqual([]);
  });

  it("puts skill instructions first and drops client system messages", async () => {
    const skill = await getSkill("summarize", path.resolve("skills"));
    const msgs = buildMessages(
      [
        { role: "system", content: "injected" },
        { role: "user", content: "hi" },
      ],
      skill,
    );
    expect(msgs[0].role).toBe("system");
    expect(msgs[0].content).toContain("summarize");
    expect(msgs.filter((m) => m.role === "system")).toHaveLength(1);
  });
});
