import { z } from "zod";
import type { ToolDef } from "./types";

const FUNCS: Record<string, (...a: number[]) => number> = {
  sqrt: Math.sqrt, abs: Math.abs, round: Math.round, floor: Math.floor, ceil: Math.ceil,
  ln: Math.log, log: Math.log10, log2: Math.log2, exp: Math.exp,
  sin: Math.sin, cos: Math.cos, tan: Math.tan, asin: Math.asin, acos: Math.acos, atan: Math.atan,
  min: Math.min, max: Math.max, pow: Math.pow,
};
const CONSTS: Record<string, number> = { pi: Math.PI, e: Math.E };

const TOKEN = /\s*(\d+(?:\.\d*)?(?:e[+-]?\d+)?|\.\d+(?:e[+-]?\d+)?|[A-Za-z_]\w*|\*\*|[-+*/%^(),])/y;

function tokenize(src: string): string[] {
  const out: string[] = [];
  TOKEN.lastIndex = 0;
  while (TOKEN.lastIndex < src.length) {
    if (/^\s*$/.test(src.slice(TOKEN.lastIndex))) break;
    const start = TOKEN.lastIndex;
    const m = TOKEN.exec(src);
    if (!m) throw new Error(`Unexpected character at position ${start + 1}`);
    out.push(m[1] === "**" ? "^" : m[1]);
  }
  return out;
}

/** Evaluates arithmetic without eval(): + - * / % ^, parentheses, common functions, pi, e. */
export function evaluate(expression: string): number {
  if (expression.length > 500) throw new Error("Expression too long");
  const t = tokenize(expression);
  let i = 0;
  const peek = () => t[i];
  const take = (expected?: string) => {
    const tok = t[i++];
    if (tok === undefined) throw new Error("Unexpected end of expression");
    if (expected && tok !== expected) throw new Error(`Expected "${expected}"`);
    return tok;
  };

  const expr = (): number => {
    let v = term();
    while (peek() === "+" || peek() === "-") v = take() === "+" ? v + term() : v - term();
    return v;
  };
  const term = (): number => {
    let v = unary();
    while (peek() === "*" || peek() === "/" || peek() === "%") {
      const op = take();
      const r = unary();
      v = op === "*" ? v * r : op === "/" ? v / r : v % r;
    }
    return v;
  };
  const unary = (): number => {
    if (peek() === "-") return take(), -unary();
    if (peek() === "+") return take(), unary();
    return power();
  };
  const power = (): number => {
    const base = primary();
    return peek() === "^" ? (take(), base ** unary()) : base; // right-associative
  };
  const primary = (): number => {
    const tok = take();
    if (tok === "(") {
      const v = expr();
      take(")");
      return v;
    }
    if (/^[\d.]/.test(tok)) return Number(tok);
    const name = tok.toLowerCase();
    if (name in CONSTS && peek() !== "(") return CONSTS[name];
    if (name in FUNCS) {
      take("(");
      const args = [expr()];
      while (peek() === ",") take(), args.push(expr());
      take(")");
      return FUNCS[name](...args);
    }
    throw new Error(`Unknown name "${tok}"`);
  };

  const result = expr();
  if (i < t.length) throw new Error(`Unexpected "${t[i]}"`);
  if (!Number.isFinite(result)) throw new Error("Result is not a finite number");
  return result;
}

const Args = z.object({ expression: z.string().min(1).max(500) });

export const calculator: ToolDef<z.infer<typeof Args>> = {
  name: "calculator",
  description:
    "Evaluate an arithmetic expression exactly. Supports + - * / % ^, parentheses, sqrt, abs, round, floor, ceil, ln, log, log2, exp, trig, min, max, pow, pi, e. Use it instead of doing math in your head.",
  schema: Args,
  describe: ({ expression }) => `Calculating ${expression.length > 40 ? expression.slice(0, 40) + "…" : expression}`,
  async run({ expression }) {
    return `${expression} = ${evaluate(expression)}`;
  },
};
