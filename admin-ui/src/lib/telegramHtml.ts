// Renders the safe subset of HTML that Telegram accepts in messages, the same way Telegram does:
// recognized tags become structured nodes, everything else (unknown tags, malformed or unbalanced
// markup) is kept as literal text. The output is a plain data tree with no HTML strings, so the React
// layer can map it to elements and let React escape text — there is no dangerouslySetInnerHTML and no
// XSS surface, even for the operator's own input.

export type TgMark = "bold" | "italic" | "underline" | "strike" | "code" | "pre" | "blockquote" | "spoiler";

export type TgNode =
  | { type: "text"; value: string }
  | { type: "link"; href: string; children: TgNode[] }
  | { type: "mark"; mark: TgMark; children: TgNode[] };

const TAG_TO_MARK: Record<string, TgMark | undefined> = {
  b: "bold",
  strong: "bold",
  i: "italic",
  em: "italic",
  u: "underline",
  ins: "underline",
  s: "strike",
  strike: "strike",
  del: "strike",
  code: "code",
  pre: "pre",
  blockquote: "blockquote",
  "tg-spoiler": "spoiler",
};

// No whitespace allowed between "<" and the tag name, so prose like "a < b > c" stays literal text.
const OPEN_RE = /<([a-zA-Z][a-zA-Z0-9-]*)((?:[^<>"']|"[^"]*"|'[^']*')*)>/y;
const CLOSE_RE = /<\/([a-zA-Z][a-zA-Z0-9-]*)\s*>/y;

type Frame =
  | { kind: "root"; children: TgNode[] }
  | { kind: "mark"; mark: TgMark; children: TgNode[] }
  | { kind: "link"; href: string | null; children: TgNode[] };

export function parseTelegramHtml(input: string): TgNode[] {
  const root: Frame = { kind: "root", children: [] };
  const stack: Frame[] = [root];
  let i = 0;
  let textStart = 0;

  const top = (): Frame => stack[stack.length - 1];
  const flushText = (end: number): void => {
    if (end > textStart) {
      top().children.push({ type: "text", value: decodeEntities(input.slice(textStart, end)) });
    }
  };

  while (i < input.length) {
    if (input[i] !== "<") {
      i++;
      continue;
    }

    CLOSE_RE.lastIndex = i;
    const close = CLOSE_RE.exec(input);
    if (close) {
      const target = findOpenFrame(stack, close[1].toLowerCase());
      if (target !== -1) {
        flushText(i);
        closeDownTo(stack, target);
        i = CLOSE_RE.lastIndex;
        textStart = i;
        continue;
      }
    }

    OPEN_RE.lastIndex = i;
    const open = OPEN_RE.exec(input);
    if (open) {
      const frame = openFrameFor(open[1].toLowerCase(), open[2] ?? "");
      if (frame) {
        flushText(i);
        stack.push(frame);
        i = OPEN_RE.lastIndex;
        textStart = i;
        continue;
      }
    }

    i++; // a lone "<" or an unsupported tag: leave it in the text run as a literal
  }

  flushText(input.length);
  while (stack.length > 1) {
    const frame = stack.pop();
    if (!frame) break;
    appendClosed(frame, top());
  }
  return root.children;
}

function openFrameFor(name: string, attrs: string): Frame | null {
  if (name === "a") {
    return { kind: "link", href: sanitizeHref(extractHref(attrs)), children: [] };
  }
  if (name === "span") {
    return /class\s*=\s*("[^"]*\btg-spoiler\b[^"]*"|'[^']*\btg-spoiler\b[^']*')/i.test(attrs)
      ? { kind: "mark", mark: "spoiler", children: [] }
      : null;
  }
  const mark = TAG_TO_MARK[name];
  return mark ? { kind: "mark", mark, children: [] } : null;
}

function findOpenFrame(stack: Frame[], name: string): number {
  const mark = TAG_TO_MARK[name];
  for (let k = stack.length - 1; k >= 1; k--) {
    const frame = stack[k];
    if (name === "a" && frame.kind === "link") return k;
    if (name === "span" && frame.kind === "mark" && frame.mark === "spoiler") return k;
    if (mark && frame.kind === "mark" && frame.mark === mark) return k;
  }
  return -1;
}

// Closes every open frame from the top down to and including `target`, so inner unclosed tags are
// wrapped rather than dropped (lenient, like a forgiving renderer rather than a strict parser).
function closeDownTo(stack: Frame[], target: number): void {
  while (stack.length - 1 >= target) {
    const frame = stack.pop();
    if (!frame) break;
    appendClosed(frame, stack[stack.length - 1]);
  }
}

function appendClosed(frame: Frame, parent: Frame): void {
  if (frame.kind === "mark") {
    parent.children.push({ type: "mark", mark: frame.mark, children: frame.children });
  } else if (frame.kind === "link") {
    if (frame.href) {
      parent.children.push({ type: "link", href: frame.href, children: frame.children });
    } else {
      // Unsupported/unsafe href: keep the link text, drop the link itself.
      parent.children.push(...frame.children);
    }
  }
}

function extractHref(attrs: string): string {
  const match = /href\s*=\s*("([^"]*)"|'([^']*)'|([^\s"'>]+))/i.exec(attrs);
  if (!match) return "";
  return match[2] ?? match[3] ?? match[4] ?? "";
}

function sanitizeHref(raw: string): string | null {
  const href = decodeEntities(raw).trim();
  return /^(https?:\/\/|tg:\/\/)/i.test(href) ? href : null;
}

function decodeEntities(value: string): string {
  return value
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#0*39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}
