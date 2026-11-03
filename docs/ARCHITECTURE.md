# Architecture

```
Browser ──(Google OAuth)──▶ Next.js app ──(HTTP, localhost)──▶ Ollama ──▶ model weights on disk
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
   prepends its instructions as the system message, and streams Ollama's output back as plain text.
5. The browser appends the stream to the conversation and saves it to `localStorage`.

## Privacy choices

- Ollama is bound to `127.0.0.1` in `docker-compose.yml`, so the model API isn't reachable from the network.
- The server doesn't log or store prompts or replies.
- The only third party involved is Google, and only for sign-in.

## Planned next steps

- Skills that call tools (web fetch, file read, calculator) via Ollama tool calling
- Optional encrypted server-side history for multi-device use
- Markdown rendering and code highlighting in replies
- Image/file attachments for multimodal models
- Dockerfile for the web app so the whole stack starts with one command
