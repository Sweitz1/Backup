"use client";

import { useEffect, useState } from "react";
import type { SkillSummary } from "@/lib/skills/types";

export default function SkillPicker({
  value,
  onChange,
}: {
  value: string;
  onChange: (name: string) => void;
}) {
  const [skills, setSkills] = useState<SkillSummary[]>([]);

  useEffect(() => {
    fetch("/api/skills")
      .then((r) => r.json())
      .then((d) => setSkills(d.skills ?? []))
      .catch(() => setSkills([]));
  }, []);

  const active = skills.find((s) => s.name === value);

  return (
    <select
      className="rounded border border-[var(--border)] bg-[var(--panel)] px-2 py-1"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      title={active ? `${active.description}${active.tools?.length ? `\nTools: ${active.tools.join(", ")}` : ""}` : undefined}
      aria-label="Skill"
    >
      <option value="">No skill</option>
      {skills.map((s) => (
        <option key={s.name} value={s.name}>
          {s.name}
        </option>
      ))}
    </select>
  );
}
