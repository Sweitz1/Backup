import fs from "node:fs/promises";
import path from "node:path";
import { z } from "zod";
import type { ToolDef } from "./types";

const MAX_FILE_BYTES = 200_000;
const MAX_ENTRIES = 200;

/** The only folder the model may read. Everything outside it is refused. */
export function filesRoot(): string {
  return path.resolve(process.env.FILES_DIR ?? path.join(process.cwd(), "data", "files"));
}

/** Resolves `rel` inside the files folder, following symlinks, and refuses anything that escapes it. */
export async function resolveInside(rel: string): Promise<{ root: string; target: string }> {
  let root: string;
  try {
    root = await fs.realpath(filesRoot());
  } catch {
    throw new Error("The files folder doesn't exist. Create it or set FILES_DIR.");
  }
  let target: string;
  try {
    target = await fs.realpath(path.resolve(root, rel));
  } catch {
    throw new Error(`Not found: ${rel}`);
  }
  if (target !== root && !target.startsWith(root + path.sep)) {
    throw new Error("Path is outside the files folder");
  }
  return { root, target };
}

const ListArgs = z.object({ dir: z.string().max(500).default(".") });

export const listFiles: ToolDef<z.infer<typeof ListArgs>> = {
  name: "list_files",
  description: "List files and folders in the user's shared files folder. `dir` is relative to that folder.",
  schema: ListArgs,
  describe: ({ dir }) => `Listing ${dir === "." ? "files" : dir}`,
  async run({ dir }) {
    const { root, target } = await resolveInside(dir);
    const entries = await fs.readdir(target, { withFileTypes: true });
    if (entries.length === 0) return "(empty folder)";
    const lines = await Promise.all(
      entries.slice(0, MAX_ENTRIES).map(async (e) => {
        const rel = path.relative(root, path.join(target, e.name));
        if (e.isDirectory()) return `${rel}/`;
        const { size } = await fs.stat(path.join(target, e.name)).catch(() => ({ size: 0 }));
        return `${rel}  (${size} bytes)`;
      }),
    );
    const more = entries.length > MAX_ENTRIES ? `\n… and ${entries.length - MAX_ENTRIES} more` : "";
    return lines.sort().join("\n") + more;
  },
};

const ReadArgs = z.object({ path: z.string().min(1).max(500) });

export const readFile: ToolDef<z.infer<typeof ReadArgs>> = {
  name: "read_file",
  description: "Read a text file from the user's shared files folder. `path` is relative to that folder.",
  schema: ReadArgs,
  describe: ({ path: p }) => `Reading ${p}`,
  async run({ path: rel }) {
    const { target } = await resolveInside(rel);
    const stat = await fs.stat(target);
    if (!stat.isFile()) throw new Error(`${rel} is not a file`);
    const handle = await fs.open(target, "r");
    try {
      const buf = Buffer.alloc(Math.min(stat.size, MAX_FILE_BYTES));
      await handle.read(buf, 0, buf.length, 0);
      if (buf.includes(0)) throw new Error(`${rel} looks like a binary file`);
      const cut = stat.size > MAX_FILE_BYTES ? "\n\n[file truncated]" : "";
      return buf.toString("utf8") + cut;
    } finally {
      await handle.close();
    }
  },
};
