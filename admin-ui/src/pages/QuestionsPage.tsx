import { useState, type ReactElement } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Pencil, Plus, Power } from "lucide-react";
import type { QuestionQuery } from "@/lib/api";
import { useCategories, useLanguages, useQuestion, useQuestions, useSetQuestionActive } from "@/lib/queries";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Pagination } from "@/components/Pagination";
import { QuestionFormDialog } from "@/components/QuestionFormDialog";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { categoryLabel, cn } from "@/lib/utils";

const DIFFICULTIES = ["easy", "medium", "hard"];
const ALL = "all";
const PAGE_SIZE = 20;

export function QuestionsPage(): ReactElement {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const categories = useCategories();
  const toggle = useSetQuestionActive();
  const languages = useLanguages().data ?? [];
  const [creating, setCreating] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const query: QuestionQuery = {
    q: params.get("q") ?? "",
    category: params.get("category") ?? "",
    difficulty: params.get("difficulty") ?? "",
    language: params.get("language") ?? "",
    page: Number(params.get("page") ?? "0"),
    size: PAGE_SIZE,
  };
  const questions = useQuestions(query);
  const categoryNames = new Map((categories.data ?? []).map((category) => [category.slug, categoryLabel(category)]));

  function setParam(key: string, value: string): void {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (value) {
          next.set(key, value);
        } else {
          next.delete(key);
        }
        if (key !== "page") {
          next.delete("page");
        }
        return next;
      },
      { replace: true },
    );
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Questions</h1>
        <Button size="sm" onClick={() => setCreating(true)}>
          <Plus className="size-4" /> New question
        </Button>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Input
          className="max-w-xs"
          placeholder="Search text…"
          aria-label="Search questions"
          value={query.q}
          onChange={(event) => setParam("q", event.target.value)}
        />
        <FilterSelect label="Category" value={query.category ?? ""}
          options={(categories.data ?? []).map((category) => ({ value: category.slug, label: categoryLabel(category) }))}
          onChange={(value) => setParam("category", value)} />
        <FilterSelect label="Difficulty" value={query.difficulty ?? ""} options={toOptions(DIFFICULTIES)} onChange={(value) => setParam("difficulty", value)} />
        <FilterSelect label="Language" value={query.language ?? ""} options={toOptions(languages.map((language) => language.code))} onChange={(value) => setParam("language", value)} />
      </div>

      {questions.isError ? (
        <ErrorState onRetry={() => void questions.refetch()} />
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-16 text-right">ID</TableHead>
                <TableHead>Text</TableHead>
                <TableHead className="w-36">Category</TableHead>
                <TableHead className="w-28">Difficulty</TableHead>
                <TableHead className="w-20">Lang</TableHead>
                <TableHead className="w-28" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {questions.isPending ? (
                <SkeletonRows />
              ) : questions.data.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="py-12 text-center text-muted-foreground">
                    No questions match your filters.
                  </TableCell>
                </TableRow>
              ) : (
                questions.data.content.map((question) => (
                  <TableRow
                    key={question.id}
                    className={cn("cursor-pointer", !question.active && "opacity-50")}
                    onClick={() => navigate(`/questions/${question.id}`)}
                  >
                    <TableCell className="text-right tabular-nums text-muted-foreground">{question.id}</TableCell>
                    <TableCell className="max-w-md">
                      <div className="flex items-center gap-2">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="block truncate text-left">{question.text}</span>
                          </TooltipTrigger>
                          <TooltipContent>{question.text}</TooltipContent>
                        </Tooltip>
                        {!question.active ? <Badge variant="outline" className="shrink-0">Disabled</Badge> : null}
                      </div>
                    </TableCell>
                    <TableCell className="capitalize">{categoryNames.get(question.category) ?? question.category}</TableCell>
                    <TableCell className="capitalize">{question.difficulty}</TableCell>
                    <TableCell className="uppercase text-muted-foreground">{question.language}</TableCell>
                    <TableCell onClick={(event) => event.stopPropagation()}>
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" aria-label={`Edit question ${question.id}`}
                          onClick={() => setEditingId(question.id)}>
                          <Pencil className="size-4" />
                        </Button>
                        <Button variant="ghost" size="icon" disabled={toggle.isPending}
                          aria-label={`${question.active ? "Disable" : "Enable"} question ${question.id}`}
                          onClick={() => toggle.mutate({ id: question.id, active: !question.active })}>
                          <Power className={cn("size-4", question.active ? "text-primary" : "text-muted-foreground")} />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          {questions.data ? (
            <Pagination
              page={query.page ?? 0}
              totalPages={questions.data.totalPages}
              totalElements={questions.data.totalElements}
              onPageChange={(page) => setParam("page", String(page))}
            />
          ) : null}
        </>
      )}

      {creating ? <QuestionFormDialog mode="create" onClose={() => setCreating(false)} /> : null}
      {editingId !== null ? <EditQuestionDialog id={editingId} onClose={() => setEditingId(null)} /> : null}
    </div>
  );
}

function EditQuestionDialog({ id, onClose }: { id: number; onClose: () => void }): ReactElement {
  const question = useQuestion(id);
  if (question.data) {
    return <QuestionFormDialog mode="edit" question={question.data} onClose={onClose} />;
  }
  return (
    <Dialog open onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit question</DialogTitle>
        </DialogHeader>
        {question.isError ? (
          <p className="text-sm text-destructive">Failed to load the question.</p>
        ) : (
          <Skeleton className="h-48 w-full" />
        )}
      </DialogContent>
    </Dialog>
  );
}

function FilterSelect({ label, value, options, onChange }: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
}): ReactElement {
  return (
    <Select value={value || ALL} onValueChange={(next) => onChange(next === ALL ? "" : next)}>
      <SelectTrigger className="w-44">
        <SelectValue placeholder={label} />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={ALL}>All {label.toLowerCase()}</SelectItem>
        {options.map((option) => (
          <SelectItem key={option.value} value={option.value} className="capitalize">
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

function toOptions(values: string[]): { value: string; label: string }[] {
  return values.map((value) => ({ value, label: value }));
}

function SkeletonRows(): ReactElement {
  return (
    <>
      {Array.from({ length: 8 }).map((_, index) => (
        <TableRow key={index}>
          <TableCell className="text-right"><Skeleton className="ml-auto h-4 w-8" /></TableCell>
          <TableCell><Skeleton className="h-4 w-full max-w-md" /></TableCell>
          <TableCell><Skeleton className="h-4 w-20" /></TableCell>
          <TableCell><Skeleton className="h-4 w-16" /></TableCell>
          <TableCell><Skeleton className="h-4 w-8" /></TableCell>
          <TableCell><Skeleton className="ml-auto h-4 w-16" /></TableCell>
        </TableRow>
      ))}
    </>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }): ReactElement {
  return (
    <div className="flex flex-col items-center gap-3 rounded-md border border-dashed py-12 text-sm text-muted-foreground">
      <span>Failed to load questions.</span>
      <Button variant="outline" size="sm" onClick={onRetry}>Retry</Button>
    </div>
  );
}
