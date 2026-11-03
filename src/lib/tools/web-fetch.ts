import http from "node:http";
import https from "node:https";
import { z } from "zod";
import { assertPublicUrl, safeLookup } from "./net-safety";
import type { ToolDef } from "./types";

const MAX_BYTES = 1_000_000; // download cap
const MAX_CHARS = 20_000; // text handed to the model
const MAX_REDIRECTS = 3;
const TIMEOUT_MS = 10_000;

type Fetched = { status: number; contentType: string; body: string; url: string; truncated: boolean };

function get(url: URL, redirectsLeft: number): Promise<Fetched> {
  assertPublicUrl(url);
  const client = url.protocol === "https:" ? https : http;

  return new Promise((resolve, reject) => {
    const req = client.request(
      url,
      {
        method: "GET",
        lookup: safeLookup,
        timeout: TIMEOUT_MS,
        headers: {
          "User-Agent": "PrivateLLM/0.1 (self-hosted)",
          Accept: "text/html,text/plain,application/json;q=0.9,*/*;q=0.5",
          "Accept-Encoding": "identity",
        },
      },
      (res) => {
        const status = res.statusCode ?? 0;
        if (status >= 300 && status < 400 && res.headers.location) {
          res.resume();
          if (redirectsLeft <= 0) return reject(new Error("Too many redirects"));
          let next: URL;
          try {
            next = new URL(res.headers.location, url);
          } catch {
            return reject(new Error("Bad redirect"));
          }
          return get(next, redirectsLeft - 1).then(resolve, reject);
        }

        const chunks: Buffer[] = [];
        let size = 0;
        let truncated = false;
        let settled = false;
        res.on("data", (chunk: Buffer) => {
          if (size + chunk.length > MAX_BYTES) {
            chunks.push(chunk.subarray(0, MAX_BYTES - size));
            size = MAX_BYTES;
            truncated = true;
            res.destroy();
          } else {
            chunks.push(chunk);
            size += chunk.length;
          }
        });
        res.on("close", () => {
          if (settled) return;
          settled = true;
          resolve({
            status,
            contentType: String(res.headers["content-type"] ?? ""),
            body: Buffer.concat(chunks).toString("utf8"),
            url: url.toString(),
            truncated,
          });
        });
        res.on("error", (err) => {
          if (!truncated && !settled) {
            settled = true;
            reject(err);
          }
        });
      },
    );
    req.on("timeout", () => req.destroy(new Error("Timed out")));
    req.on("error", reject);
    req.end();
  });
}

const ENTITIES: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: " " };

/** Crude but dependency-free HTML → readable text. */
export function htmlToText(html: string): string {
  return html
    .replace(/<(script|style|noscript|svg|template)[\s\S]*?<\/\1>/gi, " ")
    .replace(/<!--[\s\S]*?-->/g, " ")
    .replace(/<\/(p|div|li|h[1-6]|tr|br|section|article)>|<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, " ")
    .replace(/&(#\d+|#x[0-9a-f]+|[a-z]+);/gi, (m, e: string) => {
      if (e[0] === "#") {
        const code = e[1].toLowerCase() === "x" ? parseInt(e.slice(2), 16) : parseInt(e.slice(1), 10);
        return Number.isFinite(code) && code > 0 && code < 0x110000 ? String.fromCodePoint(code) : m;
      }
      return ENTITIES[e.toLowerCase()] ?? m;
    })
    .replace(/[ \t\f\v]+/g, " ")
    .replace(/\s*\n\s*/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

const Args = z.object({ url: z.string().url().max(2000) });

export const webFetch: ToolDef<z.infer<typeof Args>> = {
  name: "web_fetch",
  description:
    "Download a public web page (http/https) and return its readable text. Use for current information or when the user gives a link.",
  schema: Args,
  enabled: () => process.env.ENABLE_WEB_FETCH === "true",
  external: true,
  detail: ({ url }) => url,
  describe: ({ url }) => {
    try {
      return `Fetching ${new URL(url).hostname}`;
    } catch {
      return "Fetching a page";
    }
  },
  async run({ url }) {
    const res = await get(new URL(url), MAX_REDIRECTS);
    if (res.status >= 400) throw new Error(`HTTP ${res.status} from ${res.url}`);
    const type = res.contentType.toLowerCase();
    if (type && !/^(text\/|application\/(json|xml|xhtml\+xml|rss\+xml|atom\+xml))/.test(type)) {
      throw new Error(`Unsupported content type: ${type.split(";")[0]}`);
    }
    let text = type.includes("html") || /^\s*<!doctype html|^\s*<html/i.test(res.body) ? htmlToText(res.body) : res.body;
    let cut = res.truncated;
    if (text.length > MAX_CHARS) {
      text = text.slice(0, MAX_CHARS);
      cut = true;
    }
    return `URL: ${res.url}\n\n${text}${cut ? "\n\n[content truncated]" : ""}`;
  },
};
