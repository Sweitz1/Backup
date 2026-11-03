"use client";

import { useEffect, useRef, useState } from "react";
import SkillPicker from "./SkillPicker";
import type { StreamEvent } from "@/lib/llm/events";

type ToolStep = {
  id: number;
  label: string;
  status: "approval" | "start" | "done" | "error" | "denied";
  detail?: string; // what will be sent, shown while waiting for approval
  runId?: string;
};
type Msg = { role: "user" | "assistant"; content: string; tools?: ToolStep[] };

const HISTORY_KEY = "private-llm:history"; // chat history lives only in this browser
const CONFIRM_KEY = "private-llm:confirm-external";

const STATUS_ICON: Record<ToolStep["status"], string> = {
  approval: "⏸",
  start: "⏳",
  done: "✓",
  error: "⚠",
  denied: "✕",
};

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
  const [confirmExternal, setConfirmExternal] = useState(true);
  const [deciding, setDeciding] = useState<Set<string>>(new Set());
  const abortRef = useRef<AbortController | null>(null);
  const confirmRef = useRef(true); // read inside the stream loop, which outlives renders

  useEffect(() => {
    setMessages(loadHistory());
    try {
      const stored = localStorage.getItem(CONFIRM_KEY) !== "false";
      setConfirmExternal(stored);
      confirmRef.current = stored;
    } catch {
      /* default: ask */
    }
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

  function updateConfirm(value: boolean) {
    setConfirmExternal(value);
    confirmRef.current = value;
    try {
      localStorage.setItem(CONFIRM_KEY, String(value));
    } catch {
      /* in-memory only */
    }
  }

  /** Allow or deny a paused tool call. "always" also stops asking for future requests. */
  async function decide(step: ToolStep, allow: boolean, always = false) {
    if (!step.runId) return;
    const key = `${step.runId}:${step.id}`;
    setDeciding((s) => new Set(s).add(key));
    if (always) updateConfirm(false);
    try {
      const res = await fetch("/api/chat/approve", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ runId: step.runId, id: step.id, allow }),
      });
      if (!res.ok) setError((await res.json().catch(() => null))?.error ?? `Error ${res.status}`);
    } catch {
      setError("Couldn't send your answer to the server.");
    }
  }

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
          confirmExternal,
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
          const update: ToolStep =
            ev.type === "approval"
              ? { id: ev.id, label: ev.label, status: "approval", detail: ev.detail, runId: ev.runId }
              : { id: ev.id, label: ev.label, status: ev.status };
          const existing = steps.find((s) => s.id === ev.id);
          if (existing) Object.assign(existing, update, ev.type === "approval" ? {} : { detail: undefined });
          else steps.push(update);
          // Turned off mid-reply (e.g. "Always allow"): approve the rest automatically.
          if (ev.type === "approval" && !confirmRef.current) void decide(update, true);
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
        <label className="flex items-center gap-1 text-[var(--muted)]" title="Ask before searching the web or opening a page">
          <input type="checkbox" checked={confirmExternal} onChange={(e) => updateConfirm(e.target.checked)} />
          Ask before web access
        </label>
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
                {m.tools.map((t) => {
                  const waiting = t.status === "approval" && busy && i === messages.length - 1;
                  const locked = deciding.has(`${t.runId}:${t.id}`);
                  return (
                    <li key={t.id}>
                      {STATUS_ICON[t.status]} {t.label}
                      {t.status === "denied" && " (declined)"}
                      {waiting && (
                        <div className="mt-1 rounded-md border border-[var(--border)] p-2 text-[var(--fg)]">
                          <p className="mb-1">Allow this request to leave your machine?</p>
                          <code className="mb-2 block break-all rounded bg-[var(--bg)] px-1.5 py-1">{t.detail}</code>
                          <div className="flex flex-wrap gap-2">
                            <button
                              type="button"
                              disabled={locked}
                              className="rounded bg-[var(--accent)] px-3 py-1 text-white disabled:opacity-50"
                              onClick={() => decide(t, true)}
                            >
                              Allow
                            </button>
                            <button
                              type="button"
                              disabled={locked}
                              className="rounded border border-[var(--border)] px-3 py-1 disabled:opacity-50"
                              onClick={() => decide(t, false)}
                            >
                              Deny
                            </button>
                            <button
                              type="button"
                              disabled={locked}
                              className="px-1 underline disabled:opacity-50"
                              onClick={() => decide(t, true, true)}
                            >
                              Always allow
                            </button>
                          </div>
                        </div>
                      )}
                    </li>
                  );
                })}
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
