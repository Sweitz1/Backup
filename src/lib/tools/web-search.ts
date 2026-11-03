import { z } from "zod";
import type { ToolDef } from "./types";

const TIMEOUT_MS = 10_000;
const SNIPPET_CHARS = 300;

/** Your self-hosted SearXNG instance, e.g. http://127.0.0.1:8080. Unset = tool off. */
export function searxngUrl(): URL | undefined {
  const raw = process.env.SEARXNG_URL?.trim();
  if (!raw) return undefined;
  try {
    const url = new URL(raw);
    return url.protocol === "http:" || url.protocol === "https:" ? url : undefined;
  } catch {
    return undefined;
  }
}

type SearxResult = { title?: unknown; url?: unknown; content?: unknown; publishedDate?: unknown };
export type SearchHit = { title: string; url: string; snippet: string; date?: string };

/** Keeps only well-formed http(s) results, de-duplicated by URL. */
export function cleanResults(raw: unknown, max: number): SearchHit[] {
  const list = (raw as { results?: unknown })?.results;
  if (!Array.isArray(list)) return [];
  const seen = new Set<string>();
  const hits: SearchHit[] = [];
  for (const r of list as SearxResult[]) {
    if (typeof r?.url !== "string") continue;
    let url: URL;
    try {
      url = new URL(r.url);
    } catch {
      continue;
    }
    if (url.protocol !== "http:" && url.protocol !== "https:") continue;
    const key = url.toString();
    if (seen.has(key)) continue;
    seen.add(key);
    const snippet = typeof r.content === "string" ? r.content.replace(/\s+/g, " ").trim() : "";
    hits.push({
      title: typeof r.title === "string" && r.title.trim() ? r.title.trim() : url.hostname,
      url: key,
      snippet: snippet.length > SNIPPET_CHARS ? snippet.slice(0, SNIPPET_CHARS) + "…" : snippet,
      date: typeof r.publishedDate === "string" ? r.publishedDate.slice(0, 10) : undefined,
    });
    if (hits.length >= max) break;
  }
  return hits;
}

export function formatResults(query: string, hits: SearchHit[]): string {
  if (hits.length === 0) return `No results for "${query}". Try different or fewer keywords.`;
  return [
    `Search results for "${query}":`,
    ...hits.map(
      (h, i) => `\n${i + 1}. ${h.title}${h.date ? ` (${h.date})` : ""}\n   ${h.url}${h.snippet ? `\n   ${h.snippet}` : ""}`,
    ),
    "\nSnippets are short and may be outdated; use web_fetch on a result to read the page.",
  ].join("\n");
}

const Args = z.object({
  query: z.string().min(1).max(300),
  max_results: z.number().int().min(1).max(10).default(5),
  time_range: z.enum(["day", "week", "month", "year"]).optional(),
});

export const webSearch: ToolDef<z.infer<typeof Args>> = {
  name: "web_search",
  description:
    "Search the web and get a list of results (title, URL, snippet). Use it to find pages, then read the best ones with web_fetch. Use time_range for recent news.",
  schema: Args,
  enabled: () => searxngUrl() !== undefined,
  external: true,
  detail: ({ query, time_range }) => `Search: ${query}${time_range ? ` (past ${time_range})` : ""}`,
  describe: ({ query }) => `Searching “${query.length > 50 ? query.slice(0, 50) + "…" : query}”`,
  async run({ query, max_results, time_range }) {
    const base = searxngUrl();
    if (!base) throw new Error("Web search isn't configured (set SEARXNG_URL)");

    const url = new URL("search", base.toString().endsWith("/") ? base : base.toString() + "/");
    url.searchParams.set("q", query);
    url.searchParams.set("format", "json");
    if (time_range) url.searchParams.set("time_range", time_range);

    let res: Response;
    try {
      res = await fetch(url, {
        headers: { Accept: "application/json" },
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
    } catch (err) {
      throw new Error(`Search server unreachable: ${(err as Error).message}`);
    }
    if (res.status === 403) {
      throw new Error("Search server refused JSON output. Enable `json` under search.formats in SearXNG settings.yml.");
    }
    if (!res.ok) throw new Error(`Search server returned HTTP ${res.status}`);

    return formatResults(query, cleanResults(await res.json(), max_results));
  },
};
