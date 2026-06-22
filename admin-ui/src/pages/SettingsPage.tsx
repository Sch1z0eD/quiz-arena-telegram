import { useEffect, useState, type ReactElement } from "react";
import type { GameSettings } from "@/lib/api";
import { useGameSettings, useUpdateGameSettings } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

interface FieldSpec {
  key: keyof GameSettings;
  label: string;
  min: number;
  max: number;
  group: "Game" | "Duel";
}

const FIELDS: FieldSpec[] = [
  { key: "questionsPerGame", label: "Questions per game", min: 1, max: 50, group: "Game" },
  { key: "questionSeconds", label: "Question timer (seconds)", min: 5, max: 120, group: "Game" },
  { key: "basePoints", label: "Base points", min: 10, max: 10000, group: "Game" },
  { key: "lobbySeconds", label: "Lobby wait (seconds)", min: 5, max: 300, group: "Game" },
  { key: "duelSearchSeconds", label: "Duel search (seconds)", min: 10, max: 300, group: "Duel" },
  { key: "duelQuestionSeconds", label: "Duel question timer (seconds)", min: 5, max: 120, group: "Duel" },
  { key: "duelQuestionCount", label: "Duel questions", min: 1, max: 50, group: "Duel" },
  { key: "duelBasePoints", label: "Duel base points", min: 10, max: 10000, group: "Duel" },
];

type FormState = Record<keyof GameSettings, string>;

function toForm(settings: GameSettings): FormState {
  return Object.fromEntries(FIELDS.map((f) => [f.key, String(settings[f.key])])) as FormState;
}

function inRange(value: string, field: FieldSpec): boolean {
  if (value.trim() === "") return false;
  const n = Number(value);
  return Number.isInteger(n) && n >= field.min && n <= field.max;
}

export function SettingsPage(): ReactElement {
  const settings = useGameSettings();
  const update = useUpdateGameSettings();
  const [form, setForm] = useState<FormState | null>(null);

  useEffect(() => {
    if (settings.data && form === null) {
      setForm(toForm(settings.data));
    }
  }, [settings.data, form]);

  if (settings.isError) {
    return (
      <div className="flex flex-col items-center gap-3 rounded-md border border-dashed py-12 text-sm text-muted-foreground">
        <span>Failed to load settings.</span>
        <Button variant="outline" size="sm" onClick={() => void settings.refetch()}>Retry</Button>
      </div>
    );
  }
  if (settings.isPending || form === null || !settings.data) {
    return <Skeleton className="h-80 w-full max-w-xl" />;
  }

  const saved = settings.data;
  const invalid = FIELDS.some((f) => !inRange(form[f.key], f));
  const dirty = FIELDS.some((f) => String(saved[f.key]) !== form[f.key].trim());
  const canSave = !invalid && dirty && !update.isPending;

  function setField(key: keyof GameSettings, value: string): void {
    setForm((prev) => (prev ? { ...prev, [key]: value } : prev));
  }

  function save(): void {
    if (!form) return;
    const next: GameSettings = {
      questionsPerGame: Number(form.questionsPerGame),
      questionSeconds: Number(form.questionSeconds),
      basePoints: Number(form.basePoints),
      lobbySeconds: Number(form.lobbySeconds),
      duelSearchSeconds: Number(form.duelSearchSeconds),
      duelQuestionSeconds: Number(form.duelQuestionSeconds),
      duelQuestionCount: Number(form.duelQuestionCount),
      duelBasePoints: Number(form.duelBasePoints),
    };
    update.mutate(next);
  }

  return (
    <div className="max-w-xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Game settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">Changes apply to new games and duels immediately — no redeploy.</p>
      </div>

      {(["Game", "Duel"] as const).map((group) => (
        <Card key={group}>
          <CardHeader>
            <CardTitle className="text-base">{group}</CardTitle>
            <CardDescription>{group === "Game" ? "Solo and group quizzes." : "1v1 duels and matchmaking."}</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            {FIELDS.filter((f) => f.group === group).map((f) => {
              const bad = !inRange(form[f.key], f);
              return (
                <div key={f.key} className="flex flex-col gap-1.5">
                  <Label htmlFor={f.key}>{f.label}</Label>
                  <Input
                    id={f.key}
                    type="number"
                    min={f.min}
                    max={f.max}
                    value={form[f.key]}
                    aria-invalid={bad}
                    onChange={(e) => setField(f.key, e.target.value)}
                  />
                  <span className={bad ? "text-xs text-destructive" : "text-xs text-muted-foreground"}>
                    {f.min}–{f.max}
                  </span>
                </div>
              );
            })}
          </CardContent>
        </Card>
      ))}

      <div className="flex items-center gap-3">
        <Button onClick={save} disabled={!canSave}>
          {update.isPending ? "Saving…" : "Save"}
        </Button>
        {update.isError ? <span className="text-sm text-destructive">{update.error.message}</span> : null}
        {update.isSuccess && !dirty ? <span className="text-sm text-muted-foreground">Saved.</span> : null}
      </div>
    </div>
  );
}
