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
---

Instructions for the model go here, in plain Markdown.
```

Skills are re-read on each request, so you can add or edit one without restarting the app.
Invalid files are skipped and a warning is printed in the server log.
