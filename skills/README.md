# Skills

A skill is a folder here containing a `SKILL.md`. When a user picks a skill in the chat,
its instructions become the system prompt for that conversation.

```
skills/
  my-skill/
    SKILL.md
```

`SKILL.md` format:

```markdown
---
name: my-skill            # required; must match the folder name (a-z, 0-9, dashes)
description: One line shown in the skill picker.   # required
model: llama3.1           # optional; forces a specific local model
temperature: 0.4          # optional; 0–2
tools: [web_fetch, calculator]   # optional; tools the model may call
---

Instructions for the model go here, in plain Markdown.
```

## Tools

| Tool         | What it does                                              | Limits                                                                                   |
| ------------ | --------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `web_fetch`  | Downloads a public web page and returns its text          | Off unless `ENABLE_WEB_FETCH=true`. Can't reach this machine or your network. 10 s timeout, 1 MB download cap, text-type pages only |
| `list_files` | Lists the shared files folder                              | Only `FILES_DIR` (default `data/files/`); symlinks out of it are refused                  |
| `read_file`  | Reads a text file from the shared files folder             | Same folder rule; 200 KB cap; binary files refused                                        |
| `calculator` | Evaluates arithmetic exactly (`+ - * / % ^`, `sqrt`, `log`, trig, `pi`, `e`) | No code execution; 500-character limit                                  |

The model needs to support tool calling (for example `llama3.1`, `qwen2.5`, `mistral-nemo`).
A skill that lists an unknown tool is skipped with a warning; a known tool that's switched off is
simply not offered to the model.

To add a tool, write a `ToolDef` in `src/lib/tools/` (a zod schema for its arguments, a `run`
function that returns text) and register it in `src/lib/tools/registry.ts`.

Skills are re-read on each request, so you can add or edit one without restarting the app.
Invalid files are skipped and a warning is printed in the server log.
