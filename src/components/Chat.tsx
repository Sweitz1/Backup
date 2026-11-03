"use client";

import { useEffect, useRef, useState } from "react";
import SkillPicker from "./SkillPicker";
import type { StreamEvent } from "@/lib/llm/events";

type ToolStep = { id: number; label: string; status: "start" | "done" | "error" };
type Msg = { role: "user" | "assistant"; content: string; tools?: ToolStep[] };

const HISTORY_KEY = "private-llm:history"; // chat history lives only in this browser

function loadHistory(): Msg[] {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) ?? "[]");
  } catch {
    return [];
  }
}

function saveHistory(messages: Msg[]) {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(messages));
  } catch {
    /* storage unavailable — keep going in memory */
  }
}

export default function Chat() {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [input, setInput] = useState("");
  const [models, setModels] = useState<string[]>([]);
  const [model, setModel] = useState("");
  const [skill, setSkill] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    setMessages(loadHistory());
    fetch("/api/models")
      .then((r) => r.json())
      .then((d) => {
        setModels(d.models ?? []);
        setModel(d.default ?? "");
        if (d.error) setError(d.error);
      })
      .catch(() => setError("Couldn't reach the server."));
  }, []);

  useEffect(() => {
    if (!busy) saveHistory(messages);
  }, [messages, busy]);

  async function send(e: { preventDefault(): void }) {
    e.preventDefault();
    const text = input.trim();
    if (!text || busy) return;

    const next: Msg[] = [...messages, { role: "user", content: text }];
    setMessages([...next, { role: "assistant", content: "" }]);
    setInput("");
    setError("");
    setBusy(true);

    const controller = new AbortController();
    abortRef.current = controller;
    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: next.map(({ role, content }) => ({ role, content })),
          model: model || undefined,
          skill: skill || undefined,
        }),
        signal: controller.signal,
      });
      if (!res.ok || !res.body) {
        throw new Error((await res.json().catch(() => null))?.error ?? `Error ${res.status}`);
      }
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let reply = "";
      let buffer = "";
      const steps: ToolStep[] = [];
      const handle = (ev: StreamEvent) => {
        if (ev.type === "text") reply += ev.text;
        else if (ev.type === "error") setError(ev.message);
        else {
          const existing = steps.find((s) => s.id === ev.id);
          if (existing) Object.assign(existing, { label: ev.label, status: ev.status });
          else steps.push({ id: ev.id, label: ev.label, status: ev.status });
        }
      };
      for (;;) {
        const { done, value } = await reader.read();
        buffer += decoder.decode(value, { stream: !done });
        const lines = buffer.split("\n");
        buffer = done ? "" : (lines.pop() ?? "");
        for (const line of lines) {
          if (!line.trim()) continue;
          try {
            handle(JSON.parse(line) as StreamEvent);
          } catch {
            /* ignore a malformed line */
          }
        }
        setMessages([
          ...next,
          { role: "assistant", content: reply.trim(), tools: steps.length ? steps.map((s) => ({ ...s })) : undefined },
        ]);
        if (done) break;
      }
    } catch (err) {
      if ((err as Error).name !== "AbortError") {
        setError((err as Error).message);
        setMessages(next);
      }
    } finally {
      setBusy(false);
      abortRef.current = null;
    }
  }

  return (
    <section className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-wrap gap-2 py-3 text-sm">
        <select
          className="rounded border border-[var(--border)] bg-[var(--panel)] px-2 py-1"
          value={model}
          onChange={(e) => setModel(e.target.value)}
          aria-label="Model"
        >
          {models.length === 0 && <option value={model}>{model || "No models"}</option>}
          {models.map((m) => (
            <option key={m}>{m}</option>
          ))}
        </select>
        <SkillPicker value={skill} onChange={setSkill} />
        <button
          type="button"
          className="ml-auto text-[var(--muted)] underline"
          onClick={() => setMessages([])}
          disabled={busy}
        >
          New chat
        </button>
      </div>

      <ol className="min-h-0 flex-1 space-y-4 overflow-y-auto pb-4">
        {messages.map((m, i) => (
          <li
            key={i}
            className={
              m.role === "user"
                ? "ml-auto max-w-[85%] rounded-lg bg-[var(--accent)] px-3 py-2 text-white"
                : "max-w-[85%] whitespace-pre-wrap rounded-lg border border-[var(--border)] bg-[var(--panel)] px-3 py-2"
            }
          >
            {m.tools && (
              <ul className="mb-2 space-y-0.5 text-xs text-[var(--muted)]">
                {m.tools.map((t) => (
                  <li key={t.id}>
                    {t.status === "start" ? "⏳" : t.status === "done" ? "✓" : "⚠"} {t.label}
                  </li>
                ))}
              </ul>
            )}
            {m.content || (busy && i === messages.length - 1 ? "…" : "")}
          </li>
        ))}
      </ol>

      {error && <p className="pb-2 text-sm text-red-600">{error}</p>}

      <form onSubmit={send} className="flex gap-2 border-t border-[var(--border)] py-3">
        <textarea
          className="min-h-11 flex-1 resize-none rounded-lg border border-[var(--border)] bg-[var(--panel)] px-3 py-2"
          rows={1}
          value={input}
          placeholder="Message"
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) send(e);
          }}
        />
        {busy ? (
          <button type="button" className="rounded-lg border px-4" onClick={() => abortRef.current?.abort()}>
            Stop
          </button>
        ) : (
          <button className="rounded-lg bg-[var(--accent)] px-4 text-white">Send</button>
        )}
      </form>
    </section>
  );
}
