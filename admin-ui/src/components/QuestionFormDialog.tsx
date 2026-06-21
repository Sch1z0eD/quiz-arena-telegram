import { useState, type ReactElement } from "react";
import type { QuestionDetail, QuestionInput } from "@/lib/api";
import { useCategories, useCreateQuestion, useUpdateQuestion } from "@/lib/queries";
import { categoryLabel } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const DIFFICULTIES = ["easy", "medium", "hard"];
const LANGUAGES = ["en", "ru"];

type Props =
  | { mode: "create"; onClose: () => void }
  | { mode: "edit"; question: QuestionDetail; onClose: () => void };

interface FormState {
  text: string;
  options: string[];
  correctOption: number;
  category: string;
  difficulty: string;
  language: string;
}

function initialState(props: Props): FormState {
  if (props.mode === "edit") {
    const q = props.question;
    return {
      text: q.text,
      options: [...q.options],
      correctOption: q.correctOption,
      category: q.category,
      difficulty: q.difficulty,
      language: q.language,
    };
  }
  return { text: "", options: ["", "", "", ""], correctOption: 0, category: "", difficulty: "easy", language: "en" };
}

export function QuestionFormDialog(props: Props): ReactElement {
  const categories = useCategories();
  const create = useCreateQuestion();
  const update = useUpdateQuestion();
  const [form, setForm] = useState<FormState>(() => initialState(props));

  const pending = create.isPending || update.isPending;
  const error = create.error ?? update.error;
  const complete =
    form.text.trim().length > 0 &&
    form.options.every((option) => option.trim().length > 0) &&
    form.category.length > 0;

  function setOption(index: number, value: string): void {
    setForm((prev) => ({ ...prev, options: prev.options.map((option, i) => (i === index ? value : option)) }));
  }

  function submit(): void {
    const input: QuestionInput = {
      text: form.text,
      options: form.options,
      correctOption: form.correctOption,
      category: form.category,
      difficulty: form.difficulty,
      language: form.language,
    };
    if (props.mode === "create") {
      create.mutate(input, { onSuccess: props.onClose });
    } else {
      update.mutate({ id: props.question.id, input }, { onSuccess: props.onClose });
    }
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open) props.onClose(); }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{props.mode === "create" ? "New question" : "Edit question"}</DialogTitle>
          <DialogDescription>Pick the correct answer with the radio next to each option.</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="question-text">Question</Label>
            <Textarea
              id="question-text"
              value={form.text}
              disabled={pending}
              onChange={(event) => setForm((prev) => ({ ...prev, text: event.target.value }))}
            />
          </div>

          <fieldset className="flex flex-col gap-2" disabled={pending}>
            <legend className="mb-1 text-sm font-medium">Options</legend>
            {form.options.map((option, index) => (
              <div key={index} className="flex items-center gap-2">
                <input
                  type="radio"
                  name="correctOption"
                  className="size-4 accent-primary"
                  aria-label={`Mark option ${index + 1} correct`}
                  checked={form.correctOption === index}
                  onChange={() => setForm((prev) => ({ ...prev, correctOption: index }))}
                />
                <Input
                  value={option}
                  placeholder={`Option ${index + 1}`}
                  onChange={(event) => setOption(index, event.target.value)}
                />
              </div>
            ))}
          </fieldset>

          <div className="grid grid-cols-3 gap-3">
            <FormSelect label="Category" value={form.category} disabled={pending}
              options={(categories.data ?? []).map((category) => ({ value: category.slug, label: categoryLabel(category) }))}
              onChange={(value) => setForm((prev) => ({ ...prev, category: value }))} />
            <FormSelect label="Difficulty" value={form.difficulty} disabled={pending} options={toOptions(DIFFICULTIES)}
              onChange={(value) => setForm((prev) => ({ ...prev, difficulty: value }))} />
            <FormSelect label="Language" value={form.language} disabled={pending} options={toOptions(LANGUAGES)}
              onChange={(value) => setForm((prev) => ({ ...prev, language: value }))} />
          </div>
        </div>

        {error ? <p className="text-sm text-destructive">{error.message}</p> : null}

        <DialogFooter>
          <Button variant="outline" onClick={props.onClose} disabled={pending}>Cancel</Button>
          <Button onClick={submit} disabled={!complete || pending}>
            {pending ? "Saving…" : "Save"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function FormSelect({ label, value, options, disabled, onChange }: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  disabled: boolean;
  onChange: (value: string) => void;
}): ReactElement {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      <Select value={value || undefined} disabled={disabled} onValueChange={onChange}>
        <SelectTrigger>
          <SelectValue placeholder={`Select ${label.toLowerCase()}`} />
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value} className="capitalize">{option.label}</SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

function toOptions(values: string[]): { value: string; label: string }[] {
  return values.map((value) => ({ value, label: value }));
}
