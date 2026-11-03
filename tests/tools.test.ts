import { afterAll, beforeAll, describe, expect, it } from "vitest";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { evaluate } from "@/lib/tools/calculator";
import { assertPublicUrl, isBlockedAddress } from "@/lib/tools/net-safety";
import { htmlToText } from "@/lib/tools/web-fetch";
import { listFiles, readFile } from "@/lib/tools/files";
import { runTool, toOllamaTools, toolsFor } from "@/lib/tools/registry";
import { cleanResults, webSearch } from "@/lib/tools/web-search";
import http from "node:http";
import type { AddressInfo } from "node:net";

describe("calculator", () => {
  it.each([
    ["1 + 2 * 3", 7],
    ["(1 + 2) * 3", 9],
    ["2 ^ 3 ^ 2", 512],
    ["-2 ^ 2", -4],
    ["2 ** 10", 1024],
    ["sqrt(16) + abs(-3)", 7],
    ["max(1, 5, 3)", 5],
    ["10 % 4", 2],
    ["1.5e3 / 3", 500],
    ["round(pi * 100)", 314],
  ])("%s = %d", (expr, value) => {
    expect(evaluate(expr)).toBe(value);
  });

  it.each(["", "2 +", "process.exit()", "constructor", "1 / 0", "(1", "2 3"])("rejects %j", (expr) => {
    expect(() => evaluate(expr)).toThrow();
  });
});

describe("web_fetch network safety", () => {
  it.each(["127.0.0.1", "10.1.2.3", "192.168.1.1", "172.20.0.1", "169.254.169.254", "0.0.0.0", "::1", "fe80::1", "fd00::1", "::ffff:127.0.0.1", "::ffff:7f00:1"])(
    "blocks %s",
    (ip) => expect(isBlockedAddress(ip)).toBe(true),
  );

  it.each(["8.8.8.8", "1.1.1.1", "2606:4700:4700::1111", "::ffff:8.8.8.8", "::ffff:808:808"])("allows %s", (ip) =>
    expect(isBlockedAddress(ip)).toBe(false),
  );

  it.each([
    "file:///etc/passwd",
    "ftp://example.com/",
    "http://localhost:11434/api/tags",
    "http://127.0.0.1:3000/",
    "http://[::1]/",
    "http://169.254.169.254/latest/meta-data/",
    "https://user:pass@example.com/",
  ])("refuses %s", (url) => {
    expect(() => assertPublicUrl(new URL(url))).toThrow();
  });

  it("accepts a normal public URL", () => {
    expect(() => assertPublicUrl(new URL("https://example.com/page"))).not.toThrow();
  });

  it("is disabled unless ENABLE_WEB_FETCH=true", () => {
    const prev = process.env.ENABLE_WEB_FETCH;
    delete process.env.ENABLE_WEB_FETCH;
    expect(toolsFor(["web_fetch"])).toHaveLength(0);
    process.env.ENABLE_WEB_FETCH = "true";
    expect(toolsFor(["web_fetch"])).toHaveLength(1);
    process.env.ENABLE_WEB_FETCH = prev;
  });
});

describe("htmlToText", () => {
  it("strips scripts and tags and decodes entities", () => {
    const html = "<html><script>alert(1)</script><h1>Title</h1><p>A &amp; B&nbsp;&#39;C&#x27;</p></html>";
    expect(htmlToText(html)).toBe("Title\nA & B 'C'");
  });
});

describe("file tools", () => {
  let root: string;
  let outside: string;
  const prev = process.env.FILES_DIR;

  beforeAll(async () => {
    root = await fs.mkdtemp(path.join(os.tmpdir(), "files-"));
    outside = await fs.mkdtemp(path.join(os.tmpdir(), "outside-"));
    await fs.writeFile(path.join(root, "notes.txt"), "hello");
    await fs.mkdir(path.join(root, "sub"));
    await fs.writeFile(path.join(root, "sub", "a.md"), "# A");
    await fs.writeFile(path.join(root, "bin.dat"), Buffer.from([1, 0, 2]));
    await fs.writeFile(path.join(outside, "secret.txt"), "nope");
    await fs.symlink(path.join(outside, "secret.txt"), path.join(root, "link.txt"));
    process.env.FILES_DIR = root;
  });

  afterAll(() => {
    process.env.FILES_DIR = prev;
  });

  it("lists and reads files inside the folder", async () => {
    expect(await listFiles.run({ dir: "." })).toContain("notes.txt");
    expect(await readFile.run({ path: "sub/a.md" })).toBe("# A");
  });

  it.each(["../outside/secret.txt", "/etc/passwd", "link.txt"])("refuses to escape via %s", async (p) => {
    await expect(readFile.run({ path: p })).rejects.toThrow();
  });

  it("refuses binary files", async () => {
    await expect(readFile.run({ path: "bin.dat" })).rejects.toThrow(/binary/);
  });
});

describe("runTool", () => {
  const tools = toolsFor(["calculator"]);

  it("runs an offered tool", async () => {
    const r = await runTool(tools, "calculator", { expression: "6*7" });
    expect(r).toMatchObject({ ok: true, output: "6*7 = 42" });
  });

  it("refuses tools that weren't offered", async () => {
    expect((await runTool(tools, "read_file", { path: "x" })).ok).toBe(false);
  });

  it("reports bad arguments back instead of throwing", async () => {
    const r = await runTool(tools, "calculator", { expr: 1 });
    expect(r.ok).toBe(false);
    expect(r.output).toMatch(/invalid arguments/);
  });

  it("produces Ollama tool definitions with JSON Schema parameters", () => {
    const [def] = toOllamaTools(tools);
    expect(def.function.name).toBe("calculator");
    expect(def.function.parameters).toMatchObject({ type: "object", required: ["expression"] });
  });
});

describe("web_search", () => {
  let server: http.Server;
  let lastQuery: URLSearchParams | undefined;
  let status = 200;
  const prev = process.env.SEARXNG_URL;

  beforeAll(async () => {
    server = http.createServer((req, res) => {
      lastQuery = new URL(req.url ?? "/", "http://x").searchParams;
      res.writeHead(status, { "Content-Type": "application/json" });
      res.end(
        JSON.stringify({
          results: [
            { title: "Ollama", url: "https://ollama.com/", content: "Get up and running with   models.", publishedDate: "2026-10-01T00:00:00" },
            { title: "Dup", url: "https://ollama.com/", content: "same url" },
            { title: "Bad", url: "javascript:alert(1)", content: "x" },
            { title: "", url: "https://github.com/ollama/ollama", content: "" },
          ],
        }),
      );
    });
    await new Promise<void>((r) => server.listen(0, "127.0.0.1", r));
    process.env.SEARXNG_URL = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });

  afterAll(() => {
    server.close();
    process.env.SEARXNG_URL = prev;
  });

  it("is off when SEARXNG_URL is unset", () => {
    const url = process.env.SEARXNG_URL;
    delete process.env.SEARXNG_URL;
    expect(toolsFor(["web_search"])).toHaveLength(0);
    process.env.SEARXNG_URL = url;
    expect(toolsFor(["web_search"])).toHaveLength(1);
  });

  it("queries SearXNG as JSON and formats clean, de-duplicated results", async () => {
    const out = await webSearch.run({ query: "ollama", max_results: 5, time_range: "week" });
    expect(lastQuery?.get("q")).toBe("ollama");
    expect(lastQuery?.get("format")).toBe("json");
    expect(lastQuery?.get("time_range")).toBe("week");
    expect(out).toContain("1. Ollama (2026-10-01)\n   https://ollama.com/\n   Get up and running with models.");
    expect(out).toContain("2. github.com\n   https://github.com/ollama/ollama");
    expect(out).not.toContain("javascript:");
    expect(out).not.toContain("3.");
  });

  it("respects max_results", () => {
    const raw = { results: Array.from({ length: 20 }, (_, i) => ({ title: `r${i}`, url: `https://e.com/${i}` })) };
    expect(cleanResults(raw, 3)).toHaveLength(3);
    expect(cleanResults({ nope: 1 }, 3)).toEqual([]);
  });

  it("explains a 403 (JSON format disabled)", async () => {
    status = 403;
    await expect(webSearch.run({ query: "x", max_results: 5 })).rejects.toThrow(/search.formats/);
    status = 200;
  });

  it("goes through runTool with defaults filled in", async () => {
    const r = await runTool(toolsFor(["web_search"]), "web_search", { query: "ollama" });
    expect(r.ok).toBe(true);
    expect(r.label).toBe("Searching “ollama”");
  });
});

describe("approvals", async () => {
  const { waitForApproval, decide, cancelRun, pendingCount } = await import("@/lib/tools/approvals");
  const { prepareTool, isPrepared } = await import("@/lib/tools/registry");

  it("resolves with the owner's decision", async () => {
    const p = waitForApproval("me@x.com", "run-1", 0);
    expect(decide("me@x.com", "run-1", 0, true)).toBe(true);
    await expect(p).resolves.toBe(true);
    expect(pendingCount()).toBe(0);
  });

  it("ignores decisions from another user", async () => {
    const p = waitForApproval("me@x.com", "run-2", 0, 50);
    expect(decide("other@x.com", "run-2", 0, true)).toBe(false);
    await expect(p).resolves.toBe(false); // timed out → denied
  });

  it("denies everything pending when the reply is stopped", async () => {
    const a = waitForApproval("me@x.com", "run-3", 0);
    const b = waitForApproval("me@x.com", "run-3", 1);
    const other = waitForApproval("me@x.com", "run-4", 0);
    cancelRun("run-3");
    await expect(Promise.all([a, b])).resolves.toEqual([false, false]);
    expect(decide("me@x.com", "run-4", 0, true)).toBe(true);
    await expect(other).resolves.toBe(true);
  });

  it("flags only off-machine tools as external, with the exact detail", () => {
    process.env.ENABLE_WEB_FETCH = "true";
    process.env.SEARXNG_URL ??= "http://127.0.0.1:8080";
    const tools = toolsFor(["web_fetch", "web_search", "calculator"]);
    const fetchCall = prepareTool(tools, "web_fetch", { url: "https://example.com/a?b=1" });
    const searchCall = prepareTool(tools, "web_search", { query: "local llm", time_range: "week" });
    const calcCall = prepareTool(tools, "calculator", { expression: "1+1" });
    expect(isPrepared(fetchCall) && fetchCall.external && fetchCall.detail).toBe("https://example.com/a?b=1");
    expect(isPrepared(searchCall) && searchCall.detail).toBe("Search: local llm (past week)");
    expect(isPrepared(calcCall) && calcCall.external).toBe(false);
  });
});
