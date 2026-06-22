import { useState, type ReactElement, type ReactNode } from "react";
import { parseTelegramHtml, type TgNode } from "@/lib/telegramHtml";
import { cn } from "@/lib/utils";

export interface PreviewButton {
  text: string;
  url: string;
}

interface BroadcastPreviewProps {
  text: string;
  photoSrc?: string;
  buttons: PreviewButton[][];
  maxLen: number;
}

export function BroadcastPreview({ text, photoSrc, buttons, maxLen }: BroadcastPreviewProps): ReactElement {
  const trimmed = text.trim();
  const length = trimmed.length;
  const over = length > maxLen;
  const empty = !trimmed && !photoSrc;

  return (
    <div className="rounded-lg border bg-background p-3">
      <div className="overflow-hidden rounded-2xl rounded-tl-md border bg-card">
        {photoSrc ? <PreviewPhoto key={photoSrc} url={photoSrc} /> : null}
        {empty ? (
          <p className="px-3 py-6 text-center text-xs text-muted-foreground">Nothing to preview yet.</p>
        ) : trimmed ? (
          <div className="px-3 py-2 text-sm leading-relaxed break-words whitespace-pre-wrap">
            {renderNodes(parseTelegramHtml(text))}
          </div>
        ) : null}
        {buttons.length > 0 ? (
          <div className="flex flex-col gap-px border-t bg-border/60 p-px">
            {buttons.map((row, r) => (
              <div key={r} className="flex gap-px">
                {row.map((button, c) => (
                  <span
                    key={c}
                    className="flex-1 truncate bg-card px-3 py-2 text-center text-xs font-medium text-primary"
                  >
                    {button.text}
                  </span>
                ))}
              </div>
            ))}
          </div>
        ) : null}
      </div>
      <div className={cn("mt-2 text-right text-xs tabular-nums", over ? "text-destructive" : "text-muted-foreground")}>
        {length.toLocaleString()} / {maxLen.toLocaleString()}
      </div>
    </div>
  );
}

function PreviewPhoto({ url }: { url: string }): ReactElement {
  const [failed, setFailed] = useState(false);
  if (failed) {
    return (
      <div className="flex h-40 items-center justify-center bg-muted text-xs text-muted-foreground">
        Image failed to load
      </div>
    );
  }
  return <img src={url} alt="" onError={() => setFailed(true)} className="max-h-64 w-full object-cover" />;
}

function renderNodes(nodes: TgNode[]): ReactNode[] {
  return nodes.map((node, index) => renderNode(node, index));
}

function renderNode(node: TgNode, key: number): ReactNode {
  if (node.type === "text") {
    return node.value;
  }
  if (node.type === "link") {
    return (
      <a
        key={key}
        href={node.href}
        target="_blank"
        rel="noopener noreferrer"
        className="text-primary underline-offset-2 hover:underline"
      >
        {renderNodes(node.children)}
      </a>
    );
  }
  const children = renderNodes(node.children);
  switch (node.mark) {
    case "bold":
      return <strong key={key}>{children}</strong>;
    case "italic":
      return <em key={key}>{children}</em>;
    case "underline":
      return <u key={key}>{children}</u>;
    case "strike":
      return <s key={key}>{children}</s>;
    case "code":
      return <code key={key} className="rounded bg-background/70 px-1 py-0.5 font-mono text-[0.85em]">{children}</code>;
    case "pre":
      return (
        <pre key={key} className="my-1 overflow-x-auto rounded bg-background/70 p-2 font-mono text-[0.85em] whitespace-pre-wrap">
          {children}
        </pre>
      );
    case "blockquote":
      return <blockquote key={key} className="my-1 border-l-2 border-primary/50 pl-2 text-muted-foreground">{children}</blockquote>;
    case "spoiler":
      return <span key={key} className="select-none rounded bg-foreground/10 blur-[3px]">{children}</span>;
  }
}
