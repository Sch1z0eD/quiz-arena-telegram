import { useState, type ReactElement } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Check, Pencil, Power } from "lucide-react";
import { ApiError } from "@/lib/api";
import { useQuestion, useSetQuestionActive } from "@/lib/queries";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { QuestionFormDialog } from "@/components/QuestionFormDialog";
import { cn } from "@/lib/utils";

export function QuestionDetailPage(): ReactElement {
  const params = useParams();
  const question = useQuestion(Number(params.id));
  const toggle = useSetQuestionActive();
  const [editing, setEditing] = useState(false);

  return (
    <div>
      <Button variant="ghost" size="sm" asChild className="mb-4 gap-2 text-muted-foreground">
        <Link to="/questions">
          <ArrowLeft className="size-4" />
          Back to questions
        </Link>
      </Button>

      {question.isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : question.isError ? (
        <p className="text-sm text-destructive">
          {question.error instanceof ApiError && question.error.status === 404
            ? "Question not found."
            : "Failed to load question."}
        </p>
      ) : (
        <div className="space-y-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="mb-2 flex flex-wrap gap-2">
                <Badge variant="accent" className="capitalize">{question.data.category}</Badge>
                <Badge className="capitalize">{question.data.difficulty}</Badge>
                <Badge variant="outline" className="uppercase">{question.data.language}</Badge>
                {!question.data.active ? <Badge variant="outline">Disabled</Badge> : null}
              </div>
              <h1 className="text-lg font-semibold">{question.data.text}</h1>
            </div>
            <div className="flex shrink-0 gap-2">
              <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
                <Pencil className="size-4" /> Edit
              </Button>
              <Button variant="outline" size="sm" disabled={toggle.isPending}
                onClick={() => toggle.mutate({ id: question.data.id, active: !question.data.active })}>
                <Power className="size-4" /> {question.data.active ? "Disable" : "Enable"}
              </Button>
            </div>
          </div>

          <Card>
            <CardContent className="space-y-2 pt-6">
              {question.data.options.map((option, index) => (
                <div
                  key={index}
                  className={cn(
                    "flex items-center justify-between rounded-md border px-3 py-2 text-sm",
                    index === question.data.correctOption && "border-primary/50 bg-primary/10",
                  )}
                >
                  <span>{option}</span>
                  {index === question.data.correctOption ? <Check className="size-4 text-primary" /> : null}
                </div>
              ))}
            </CardContent>
          </Card>

          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard label="Answered" value={question.data.stats.answered.toLocaleString()} />
            <StatCard label="Correct" value={question.data.stats.correct.toLocaleString()} />
            <StatCard label="Accuracy" value={`${question.data.stats.accuracyPercent}%`} />
          </div>

          <p className="break-all font-mono text-xs text-muted-foreground">hash: {question.data.hash}</p>

          {editing ? (
            <QuestionFormDialog mode="edit" question={question.data} onClose={() => setEditing(false)} />
          ) : null}
        </div>
      )}
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string }): ReactElement {
  return (
    <Card>
      <CardHeader>
        <CardDescription>{label}</CardDescription>
        <CardTitle className="text-2xl tabular-nums">{value}</CardTitle>
      </CardHeader>
    </Card>
  );
}
