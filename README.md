# Private LLM

A private, self-hosted chat app for open-weight language models.

- **Local models** — runs any model you pull into [Ollama](https://ollama.com) on your own machine; prompts never leave it.
- **Google sign-in** — only the Google accounts you list in `ALLOWED_EMAILS` can get in.
- **Skills** — drop a `SKILL.md` into `skills/` to add a reusable mode (see [skills/README.md](skills/README.md)).
- **Tools** — skills can let the model search the web (via your own SearXNG), read web pages, read files from one shared folder, and do exact math. By default the chat asks you before anything leaves your machine, showing the exact URL or search query.
- **No server-side history** — conversations are kept in your browser only.

## Quick start

```bash
# 1. Configure
cp .env.example .env            # fill in AUTH_SECRET, Google OAuth ID/secret, ALLOWED_EMAILS, SEARXNG_SECRET
                                # set ENABLE_WEB_FETCH=true to let skills read web pages

# 2. Start the model server and search engine, then pull a model
docker compose up -d
docker compose exec ollama ollama pull llama3.1

# 3. Run the app
npm install
npm run dev                     # http://localhost:3000
```

Check search is working: open http://127.0.0.1:8080 and run a query. Then pick the
**researcher** skill in the app and ask something current.

### Google OAuth setup

1. Google Cloud Console → APIs & Services → Credentials → **Create OAuth client ID** (Web application).
2. Authorized redirect URI: `http://localhost:3000/api/auth/callback/google`
   (add your real domain's `/api/auth/callback/google` when you deploy).
3. Put the client ID and secret in `AUTH_GOOGLE_ID` / `AUTH_GOOGLE_SECRET`.

## Project layout

```
skills/                 Skill folders (SKILL.md each)
searxng/settings.yml    Config for the self-hosted search engine
src/
  auth.ts               Auth.js config: Google provider + email allowlist
  proxy.ts              Requires sign-in for every page and API route
  app/
    page.tsx            Chat screen
    signin/page.tsx     Sign-in screen
    api/chat/           Streams replies from the local model, runs tool calls
    api/models/         Lists models installed in Ollama
    api/skills/         Lists available skills
  components/           Chat UI, skill picker
  lib/
    llm/                Local model client, stream event types
    tools/              Tools skills can use (web_search, web_fetch, list_files, read_file, calculator)
    skills/             Skill loader, schema, prompt building
tests/                  Vitest tests
docs/ARCHITECTURE.md    How the pieces fit together
```

## Scripts

| Command             | What it does          |
| ------------------- | --------------------- |
| `npm run dev`       | Dev server            |
| `npm run build`     | Production build      |
| `npm test`          | Run tests             |
| `npm run typecheck` | TypeScript check      |
| `npm run lint`      | ESLint                |
