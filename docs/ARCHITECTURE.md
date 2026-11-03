# Architecture

```
Browser ──(Google OAuth)──▶ Next.js app ──(localhost)──▶ Ollama ──▶ model weights on disk
                                 └──(localhost)──▶ SearXNG ──▶ public search engines
   │                            │
   └─ chat history in           └─ reads skills/*/SKILL.md on each request
      localStorage only
```

## Request flow

1. `src/proxy.ts` checks the Auth.js session on every request. No session → `/signin` (pages) or `401` (API).
2. Sign-in goes through Google. `auth.ts` rejects any account that isn't verified and listed in `ALLOWED_EMAILS`.
   Sessions are JWT cookies; there's no user database.
3. The chat UI posts the conversation to `/api/chat` with an optional `model` and `skill`.
4. The route validates the body (clients may only send `user`/`assistant` turns), loads the skill,
   prepends its instructions as the system message, and streams Ollama's output back as
   newline-delimited JSON events (`src/lib/llm/events.ts`).
5. If the skill lists tools, the route runs the tool loop on the server (below).
6. The browser shows text and tool activity as they arrive and saves the conversation to `localStorage`.

## Tool loop

```
model ──tool_calls──▶ prepareTool() ──(external? wait for Allow/Deny)──▶ executeTool() ──result──▶ model ──…──▶ answer
```

- Only tools the active skill lists, and that are switched on, are offered to the model.
- `prepareTool` refuses any tool that wasn't offered and validates arguments with zod; problems go
  back to the model as text so it can recover. Output is capped at 20,000 characters.
- At most 6 model ↔ tool rounds per reply. Pressing Stop aborts the current model call.
- Tool results are added to the conversation sent to the model but aren't returned to the browser
  in full; the UI only shows a one-line label per call.

### Approval for off-machine requests

Tools marked `external` (`web_search`, `web_fetch`) pause before running when "Ask before web
access" is on (the default; the toggle is remembered per browser):

1. The chat stream sends an `approval` event with the exact URL or search query.
2. The browser shows Allow / Deny / Always allow and posts the choice to `/api/chat/approve`.
3. `src/lib/tools/approvals.ts` holds the paused call in memory, keyed by reply and call ID and
   tied to the signed-in user, so only that user can answer it. It's denied automatically after
   2 minutes, or when the user presses Stop.
4. A denied call tells the model the user declined, so it continues without it.

"Always allow" turns the toggle off and approves the remaining calls in the current reply.
The pending store is per process, so run a single app instance (the normal self-hosted setup).

### Tool safety

- **web_search** only talks to the SearXNG instance in `SEARXNG_URL` (yours, started by
  docker-compose and bound to localhost). SearXNG forwards the query to public engines without
  cookies or an account. Result URLs are filtered to http/https and de-duplicated; the model then
  reads pages through `web_fetch`, which applies the network rules below.
- **web_fetch** checks the URL (http/https, no credentials, no internal IP literals), then checks
  every DNS answer at connect time, so a hostname that resolves to `127.0.0.1`, your LAN or a
  cloud metadata address is refused even after redirects. It's off unless `ENABLE_WEB_FETCH=true`
  because it sends requests to sites the model picks.
- **read_file / list_files** resolve real paths (following symlinks) and refuse anything outside `FILES_DIR`.
- **calculator** is a hand-written parser; it never calls `eval`.

## Privacy choices

- Ollama is bound to `127.0.0.1` in `docker-compose.yml`, so the model API isn't reachable from the network.
- The server doesn't log or store prompts or replies.
- The only third parties involved are Google (sign-in) and, when web tools are on, the search
  engines SearXNG queries and the sites the model fetches.

## Planned next steps

- Optional encrypted server-side history for multi-device use
- Markdown rendering and code highlighting in replies
- Image/file attachments for multimodal models
- Dockerfile for the web app so the whole stack starts with one command
