import { useMemo, useState, type ReactElement } from "react";
import type { BroadcastMessage } from "@/lib/api";
import {
  useBroadcast,
  useBroadcastAbort,
  useBroadcastDryRun,
  useBroadcasts,
  useBroadcastStart,
  useBroadcastTest,
} from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

const LANGUAGES = ["ru", "en"];
const PAGE_SIZE = 10;

function isHttp(url: string): boolean {
  return /^https?:\/\//.test(url);
}

function formatTs(ms: number): string {
  return new Date(ms).toLocaleString();
}

export function BroadcastsPage(): ReactElement {
  const [text, setText] = useState("");
  const [photoUrl, setPhotoUrl] = useState("");
  const [buttonText, setButtonText] = useState("");
  const [buttonUrl, setButtonUrl] = useState("");
  const [segment, setSegment] = useState("all");
  const [language, setLanguage] = useState("ru");
  const [dryRun, setDryRun] = useState<{ id: number; total: number; token: string } | null>(null);
  const [typedCount, setTypedCount] = useState("");
  const [activeId, setActiveId] = useState<number | null>(null);

  const test = useBroadcastTest();
  const dry = useBroadcastDryRun();
  const start = useBroadcastStart();
  const broadcasts = useBroadcasts(0, PAGE_SIZE);

  // Any edit to the message or segment invalidates a prior dry-run, so full-send re-locks until re-run.
  function resetConfirmation(): void {
    setDryRun(null);
    setTypedCount("");
  }

  const message: BroadcastMessage = useMemo(() => ({
    text: text.trim(),
    photoUrl: photoUrl.trim() || undefined,
    button: buttonText.trim() && buttonUrl.trim() ? { text: buttonText.trim(), url: buttonUrl.trim() } : undefined,
  }), [text, photoUrl, buttonText, buttonUrl]);

  const errors = useMemo(() => {
    const list: string[] = [];
    const hasPhoto = !!message.photoUrl;
    const maxLen = hasPhoto ? 1024 : 4096;
    if (!message.text) list.push("Text is required.");
    if (message.text.length > maxLen) list.push(`Text exceeds ${maxLen} characters (${hasPhoto ? "with photo" : "no photo"}).`);
    if (hasPhoto && !isHttp(message.photoUrl as string)) list.push("Photo URL must be http(s).");
    if (Boolean(buttonText.trim()) !== Boolean(buttonUrl.trim())) list.push("A button needs both text and URL.");
    if (message.button && !isHttp(message.button.url)) list.push("Button URL must be http(s).");
    return list;
  }, [message, buttonText, buttonUrl]);

  const messageValid = errors.length === 0;
  const segmentValid = segment === "all" || (segment === "by-language" && language.length > 0);
  const fullSendReady = dryRun !== null && Number(typedCount) === dryRun.total && !start.isPending;

  function sendTest(): void {
    test.mutate(message, { onSuccess: (summary) => setActiveId(summary.id) });
  }

  function runDryRun(): void {
    dry.mutate(
      { segment, language: segment === "by-language" ? language : undefined, message },
      { onSuccess: (result) => { setDryRun(result); setTypedCount(""); } },
    );
  }

  function sendFull(): void {
    if (dryRun === null) {
      return;
    }
    const id = dryRun.id;
    start.mutate({ id, token: dryRun.token }, { onSuccess: () => { setActiveId(id); resetConfirmation(); } });
  }

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Broadcasts</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Message</CardTitle>
          <CardDescription>HTML is allowed. Send a test to yourself to see the real rendering before any blast.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Field label="Text">
            <Textarea value={text} onChange={(e) => { setText(e.target.value); resetConfirmation(); }} rows={5} />
          </Field>
          <Field label="Photo URL (optional)">
            <Input value={photoUrl} placeholder="https://…" onChange={(e) => { setPhotoUrl(e.target.value); resetConfirmation(); }} />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Button text (optional)">
              <Input value={buttonText} onChange={(e) => { setButtonText(e.target.value); resetConfirmation(); }} />
            </Field>
            <Field label="Button URL (optional)">
              <Input value={buttonUrl} placeholder="https://…" onChange={(e) => { setButtonUrl(e.target.value); resetConfirmation(); }} />
            </Field>
          </div>
          {errors.length > 0 && text.length > 0 ? (
            <ul className="text-sm text-destructive">{errors.map((e) => <li key={e}>{e}</li>)}</ul>
          ) : null}

          <div className="flex flex-wrap items-center gap-3 border-t pt-4">
            <Button onClick={sendTest} disabled={!messageValid || test.isPending}>
              {test.isPending ? "Sending…" : "Send test to me"}
            </Button>
            {test.isSuccess ? <span className="text-sm text-muted-foreground">Test sent — check Telegram.</span> : null}
            {test.isError ? <span className="text-sm text-destructive">{test.error.message}</span> : null}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Audience</CardTitle>
          <CardDescription>Recipients exclude banned and blocked users. Count is an estimate; the real set is resolved at send time.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-end gap-3">
            <Field label="Segment">
              <Select value={segment} onValueChange={(v) => { setSegment(v); resetConfirmation(); }}>
                <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All users</SelectItem>
                  <SelectItem value="by-language">By language</SelectItem>
                </SelectContent>
              </Select>
            </Field>
            {segment === "by-language" ? (
              <Field label="Language">
                <Select value={language} onValueChange={(v) => { setLanguage(v); resetConfirmation(); }}>
                  <SelectTrigger className="w-28"><SelectValue /></SelectTrigger>
                  <SelectContent>{LANGUAGES.map((l) => <SelectItem key={l} value={l} className="uppercase">{l}</SelectItem>)}</SelectContent>
                </Select>
              </Field>
            ) : null}
            <Button variant="outline" onClick={runDryRun} disabled={!messageValid || !segmentValid || dry.isPending}>
              {dry.isPending ? "Counting…" : "Dry-run (count recipients)"}
            </Button>
          </div>
          {dry.isError ? <p className="text-sm text-destructive">{dry.error.message}</p> : null}

          {dryRun !== null ? (
            <div className="space-y-3 rounded-md border border-destructive/40 bg-destructive/5 p-4">
              <p className="text-sm">
                Estimated recipients: <span className="font-semibold tabular-nums">{dryRun.total.toLocaleString()}</span>.
                To confirm, type the exact number below.
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <Input
                  className="w-40"
                  inputMode="numeric"
                  placeholder="recipient count"
                  value={typedCount}
                  onChange={(e) => setTypedCount(e.target.value.replace(/[^0-9]/g, ""))}
                />
                <Button variant="destructive" onClick={sendFull} disabled={!fullSendReady}>
                  {start.isPending ? "Starting…" : `Send to ${dryRun.total.toLocaleString()} users`}
                </Button>
              </div>
              {start.isError ? <p className="text-sm text-destructive">{start.error.message}</p> : null}
            </div>
          ) : null}
        </CardContent>
      </Card>

      {activeId !== null ? <LivePanel id={activeId} /> : null}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">History</CardTitle>
        </CardHeader>
        <CardContent>
          {broadcasts.isError ? (
            <p className="text-sm text-destructive">Failed to load history.</p>
          ) : broadcasts.isPending ? (
            <Skeleton className="h-40 w-full" />
          ) : broadcasts.data.content.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">No broadcasts yet.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-44">When</TableHead>
                  <TableHead>Segment</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Sent / Failed / Total</TableHead>
                  <TableHead className="w-24" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {broadcasts.data.content.map((b) => (
                  <TableRow key={b.id}>
                    <TableCell className="text-muted-foreground">{formatTs(b.createdAt)}</TableCell>
                    <TableCell>{b.segment}{b.language ? ` (${b.language})` : ""}</TableCell>
                    <TableCell><StatusBadge status={b.status} /></TableCell>
                    <TableCell className="text-right tabular-nums">{b.sent} / {b.failed} / {b.total}</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setActiveId(b.id)}>View</Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function LivePanel({ id }: { id: number }): ReactElement {
  const broadcast = useBroadcast(id);
  const abort = useBroadcastAbort();

  if (broadcast.isPending || !broadcast.data) {
    return <Card><CardContent className="pt-6"><Skeleton className="h-16 w-full" /></CardContent></Card>;
  }
  const b = broadcast.data;
  const done = b.sent + b.failed;
  const percent = b.total > 0 ? Math.round((done / b.total) * 100) : 0;
  const running = b.status === "RUNNING";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">Broadcast #{b.id}</CardTitle>
          <StatusBadge status={b.status} />
        </div>
        <CardDescription>{b.segment}{b.language ? ` (${b.language})` : ""}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="h-2 w-full overflow-hidden rounded bg-muted">
          <div className="h-full bg-primary transition-all" style={{ width: `${percent}%` }} />
        </div>
        <p className="text-sm tabular-nums text-muted-foreground">
          {b.sent} sent · {b.failed} failed · {b.total} total ({percent}%)
        </p>
        {running ? (
          <Button variant="destructive" size="sm" disabled={abort.isPending} onClick={() => abort.mutate(b.id)}>
            {abort.isPending ? "Aborting…" : "Abort"}
          </Button>
        ) : null}
      </CardContent>
    </Card>
  );
}

function StatusBadge({ status }: { status: string }): ReactElement {
  const variant = status === "DONE" ? "accent" : status === "RUNNING" ? "default" : "outline";
  return <Badge variant={variant}>{status}</Badge>;
}

function Field({ label, children }: { label: string; children: ReactElement }): ReactElement {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
