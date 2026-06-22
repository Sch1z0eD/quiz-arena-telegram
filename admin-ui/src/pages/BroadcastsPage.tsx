import { useMemo, useRef, useState, type ChangeEvent, type ReactElement } from "react";
import { X } from "lucide-react";
import type { BroadcastMessage } from "@/lib/api";
import {
  useBroadcast,
  useBroadcastAbort,
  useBroadcastDryRun,
  useBroadcastPhotoUpload,
  useBroadcasts,
  useBroadcastStart,
  useBroadcastTest,
  useLanguages,
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
import { BroadcastPreview, type PreviewButton } from "@/components/BroadcastPreview";
import { formatTs } from "@/lib/utils";

const PAGE_SIZE = 10;
const CAPTION_LIMIT = 1024;
const TEXT_LIMIT = 4096;
const MAX_BUTTON_ROWS = 10;
const MAX_BUTTONS_PER_ROW = 8;
const MAX_BUTTON_TEXT = 64;
const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
const PHOTO_TYPES = ["image/jpeg", "image/png", "image/webp"];

interface ButtonDraft {
  text: string;
  url: string;
}

interface UploadedPhoto {
  name: string;
  objectUrl: string;
  fileId: string;
}

function isHttp(url: string): boolean {
  return /^https?:\/\//.test(url);
}

export function BroadcastsPage(): ReactElement {
  const [text, setText] = useState("");
  const [photoUrlInput, setPhotoUrlInput] = useState("");
  const [uploadedPhoto, setUploadedPhoto] = useState<UploadedPhoto | null>(null);
  const [photoError, setPhotoError] = useState("");
  const [buttons, setButtons] = useState<ButtonDraft[][]>([]);
  const [segment, setSegment] = useState("all");
  const [language, setLanguage] = useState("ru");
  const [dryRun, setDryRun] = useState<{ id: number; total: number; token: string } | null>(null);
  const [typedCount, setTypedCount] = useState("");
  const [activeId, setActiveId] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const test = useBroadcastTest();
  const dry = useBroadcastDryRun();
  const start = useBroadcastStart();
  const upload = useBroadcastPhotoUpload();
  const broadcasts = useBroadcasts(0, PAGE_SIZE);
  const languages = useLanguages().data ?? [];

  // Any edit to the message or segment invalidates a prior dry-run, so full-send re-locks until re-run.
  function resetConfirmation(): void {
    setDryRun(null);
    setTypedCount("");
  }

  // The value we SEND is a file_id (after upload) or the typed URL; the value we DISPLAY in the preview is the
  // local object URL (a file_id is not a renderable image source) or the typed URL.
  const photoValue = uploadedPhoto ? uploadedPhoto.fileId : photoUrlInput.trim();
  const photoSrc = uploadedPhoto ? uploadedPhoto.objectUrl : photoUrlInput.trim() || undefined;

  const message: BroadcastMessage = useMemo(() => {
    const rows = buttons
      .map((row) => row.map((b) => ({ text: b.text.trim(), url: b.url.trim() })).filter((b) => b.text || b.url))
      .filter((row) => row.length > 0);
    return {
      text: text.trim(),
      photoUrl: photoValue || undefined,
      buttons: rows.length > 0 ? rows : undefined,
    };
  }, [text, photoValue, buttons]);

  const maxLen = message.photoUrl ? CAPTION_LIMIT : TEXT_LIMIT;
  const previewButtons: PreviewButton[][] = useMemo(
    () =>
      buttons
        .map((row) => row.filter((b) => b.text.trim()).map((b) => ({ text: b.text.trim(), url: b.url.trim() })))
        .filter((row) => row.length > 0),
    [buttons],
  );

  const errors = useMemo(() => {
    const list: string[] = [];
    const photoUrl = message.photoUrl;
    const hasPhoto = !!photoUrl;
    const maxLen = hasPhoto ? CAPTION_LIMIT : TEXT_LIMIT;
    if (!message.text) list.push("Text is required.");
    if (message.text.length > maxLen) list.push(`Text exceeds ${maxLen} characters (${hasPhoto ? "with photo" : "no photo"}).`);
    if (photoUrl && !uploadedPhoto && !isHttp(photoUrl)) list.push("Photo URL must be http(s).");
    if (buttons.length > MAX_BUTTON_ROWS) list.push(`Too many button rows (max ${MAX_BUTTON_ROWS}).`);
    if (buttons.some((row) => row.length > MAX_BUTTONS_PER_ROW)) list.push(`Too many buttons in a row (max ${MAX_BUTTONS_PER_ROW}).`);
    const flat = buttons.flat();
    if (flat.some((b) => Boolean(b.text.trim()) !== Boolean(b.url.trim()))) list.push("Each button needs both text and URL.");
    if (flat.some((b) => b.url.trim() && !isHttp(b.url.trim()))) list.push("Button URLs must be http(s).");
    if (flat.some((b) => b.text.trim().length > MAX_BUTTON_TEXT)) list.push(`Button text exceeds ${MAX_BUTTON_TEXT} characters.`);
    return list;
  }, [message, buttons, uploadedPhoto]);

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

  function onPickFile(event: ChangeEvent<HTMLInputElement>): void {
    const file = event.target.files?.[0];
    event.target.value = ""; // let the same file be re-picked after a clear
    if (!file) return;
    setPhotoError("");
    if (!PHOTO_TYPES.includes(file.type)) { setPhotoError("Use a JPEG, PNG or WebP image."); return; }
    if (file.size > MAX_PHOTO_BYTES) { setPhotoError("Image exceeds 5 MB."); return; }
    upload.mutate(file, {
      onSuccess: (result) => {
        setUploadedPhoto({ name: file.name, objectUrl: URL.createObjectURL(file), fileId: result.fileId });
        resetConfirmation();
      },
      onError: (error) => setPhotoError(error.message),
    });
  }

  function clearPhoto(): void {
    if (uploadedPhoto) URL.revokeObjectURL(uploadedPhoto.objectUrl);
    setUploadedPhoto(null);
    setPhotoError("");
    resetConfirmation();
  }

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Broadcasts</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Message</CardTitle>
          <CardDescription>HTML is allowed. Send a test to yourself to see the real rendering before any blast.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
            <div className="space-y-4">
              <Field label="Text">
                <Textarea value={text} onChange={(e) => { setText(e.target.value); resetConfirmation(); }} rows={5} />
              </Field>
              <Field label="Photo (optional)">
                {uploadedPhoto ? (
                  <div className="flex items-center justify-between gap-2 rounded-md border border-border/60 px-3 py-2 text-sm">
                    <span className="truncate text-muted-foreground">Uploaded: {uploadedPhoto.name}</span>
                    <Button type="button" variant="ghost" size="icon" aria-label="Remove photo" onClick={clearPhoto}>
                      <X className="size-4" />
                    </Button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <Input value={photoUrlInput} placeholder="https://… image URL" onChange={(e) => { setPhotoUrlInput(e.target.value); resetConfirmation(); }} />
                    <div className="flex flex-wrap items-center gap-2">
                      <input ref={fileInputRef} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={onPickFile} />
                      <Button type="button" variant="outline" size="sm" disabled={upload.isPending} onClick={() => fileInputRef.current?.click()}>
                        {upload.isPending ? "Uploading…" : "Upload from computer"}
                      </Button>
                      {photoError ? <span className="text-sm text-destructive">{photoError}</span> : null}
                    </div>
                  </div>
                )}
              </Field>
              <ButtonsEditor value={buttons} onChange={(next) => { setButtons(next); resetConfirmation(); }} />
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
            </div>

            <div className="lg:sticky lg:top-6 lg:self-start">
              <p className="mb-2 text-xs font-medium text-muted-foreground">Preview</p>
              <BroadcastPreview text={text} photoSrc={photoSrc} buttons={previewButtons} maxLen={maxLen} />
              <p className="mt-2 text-xs text-muted-foreground">Approximate. Send a test for the authoritative rendering.</p>
            </div>
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
                  <SelectContent>{languages.map((l) => <SelectItem key={l.code} value={l.code} className="uppercase">{l.code}</SelectItem>)}</SelectContent>
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

function ButtonsEditor({ value, onChange }: { value: ButtonDraft[][]; onChange: (next: ButtonDraft[][]) => void }): ReactElement {
  const setField = (r: number, c: number, field: keyof ButtonDraft, v: string): void =>
    onChange(value.map((row, ri) => (ri === r ? row.map((b, ci) => (ci === c ? { ...b, [field]: v } : b)) : row)));
  const addButton = (r: number): void =>
    onChange(value.map((row, ri) => (ri === r ? [...row, { text: "", url: "" }] : row)));
  const removeButton = (r: number, c: number): void =>
    onChange(value.map((row, ri) => (ri === r ? row.filter((_, ci) => ci !== c) : row)).filter((row) => row.length > 0));
  const addRow = (): void => onChange([...value, [{ text: "", url: "" }]]);

  return (
    <div className="flex flex-col gap-2">
      <Label>Buttons (optional)</Label>
      {value.map((row, r) => (
        <div key={r} className="space-y-2 rounded-md border border-border/60 p-2">
          {row.map((button, c) => (
            <div key={c} className="flex items-center gap-2">
              <Input className="flex-1" placeholder="Label" value={button.text} onChange={(e) => setField(r, c, "text", e.target.value)} />
              <Input className="flex-1" placeholder="https://…" value={button.url} onChange={(e) => setField(r, c, "url", e.target.value)} />
              <Button type="button" variant="ghost" size="icon" aria-label="Remove button" onClick={() => removeButton(r, c)}>
                <X className="size-4" />
              </Button>
            </div>
          ))}
          {row.length < MAX_BUTTONS_PER_ROW ? (
            <Button type="button" variant="outline" size="sm" onClick={() => addButton(r)}>Add button</Button>
          ) : null}
        </div>
      ))}
      {value.length < MAX_BUTTON_ROWS ? (
        <Button type="button" variant="outline" size="sm" onClick={addRow}>Add row</Button>
      ) : null}
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactElement }): ReactElement {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
