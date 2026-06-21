import type { ReactElement } from "react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { CategoryAnswerDistribution, NamedCount, Overview } from "@/lib/api";
import { useAnswerDistribution, useOverview } from "@/lib/queries";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";

export function DashboardPage(): ReactElement {
  const overview = useOverview();

  return (
    <div>
      <h1 className="mb-6 text-xl font-semibold">Dashboard</h1>
      {overview.isError ? (
        <div className="flex flex-col items-center gap-3 rounded-md border border-dashed py-12 text-sm text-muted-foreground">
          <span>Failed to load dashboard.</span>
          <Button variant="outline" size="sm" onClick={() => void overview.refetch()}>Retry</Button>
        </div>
      ) : overview.isPending ? (
        <DashboardSkeleton />
      ) : (
        <Content data={overview.data} />
      )}
    </div>
  );
}

function Content({ data }: { data: Overview }): ReactElement {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Metric label="Players" value={data.players.total} />
        <Metric label="Active players (7d)" value={data.players.active7d} />
        <Metric label="Active players (30d)" value={data.players.active30d} />
        <Metric label="Answer accuracy" value={`${data.accuracyPercent}%`} />
        <Metric label="Solo games" value={data.games.solo} />
        <Metric label="Group games" value={data.games.group} />
        <Metric label="Duels" value={data.games.duel} />
        <Metric label="Answers" value={data.totalAnswers} />
        <Metric label="Active questions" value={data.questions.active} />
        <Metric label="Inactive questions" value={data.questions.inactive} />
        <Metric label="Active categories" value={data.categories.active} />
        <Metric label="Hidden categories" value={data.categories.hidden} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Answers per day</CardTitle>
          <CardDescription>Last 30 days, bucketed by UTC day.</CardDescription>
        </CardHeader>
        <CardContent>
          {data.answersPerDay.length === 0 ? (
            <p className="py-12 text-center text-sm text-muted-foreground">No activity yet.</p>
          ) : (
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.answersPerDay} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
                  <XAxis dataKey="day" tick={{ fill: "#9aa3c0", fontSize: 11 }} tickLine={false} axisLine={false} minTickGap={16} />
                  <YAxis tick={{ fill: "#9aa3c0", fontSize: 11 }} tickLine={false} axisLine={false} allowDecimals={false} width={32} />
                  <Tooltip
                    cursor={{ fill: "rgba(255,255,255,0.05)" }}
                    contentStyle={{ background: "#1c2140", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, fontSize: 12 }}
                    labelStyle={{ color: "#e8ebf5" }}
                  />
                  <Bar dataKey="count" fill="#13d6c0" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Breakdown title="Answers by category" header="Category" rows={data.topCategories} />
        <Breakdown title="Questions by category" header="Category" rows={data.questions.byCategory} />
        <Breakdown title="Questions by difficulty" header="Difficulty" rows={data.questions.byDifficulty} />
        <Breakdown title="Questions by language" header="Language" rows={data.questions.byLanguage} />
      </div>

      <AnswerPositionCard />
    </div>
  );
}

const POSITION_COLORS: Record<string, string> = { A: "#13d6c0", B: "#6c5ce7", C: "#6b7394", D: "#9aa3c0" };

function AnswerPositionCard(): ReactElement {
  const distribution = useAnswerDistribution();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Correct-answer position by category</CardTitle>
        <CardDescription>
          Share of questions whose correct option sits at A/B/C/D in storage — an authoring-skew signal.
          The bot shuffles option order every round, so this does not affect play fairness.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {distribution.isError ? (
          <p className="py-6 text-center text-sm text-destructive">Failed to load distribution.</p>
        ) : distribution.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : distribution.data.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">No questions yet.</p>
        ) : (
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={toShares(distribution.data)} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
                <XAxis dataKey="category" tick={{ fill: "#9aa3c0", fontSize: 11 }} tickLine={false} axisLine={false} minTickGap={8} />
                <YAxis tick={{ fill: "#9aa3c0", fontSize: 11 }} tickLine={false} axisLine={false} width={36} unit="%" domain={[0, 100]} />
                <Tooltip
                  cursor={{ fill: "rgba(255,255,255,0.05)" }}
                  contentStyle={{ background: "#1c2140", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, fontSize: 12 }}
                  labelStyle={{ color: "#e8ebf5" }}
                  formatter={(value, name) => [`${value}%`, name]}
                />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                {(["A", "B", "C", "D"] as const).map((key) => (
                  <Bar key={key} dataKey={key} stackId="pos" fill={POSITION_COLORS[key]} />
                ))}
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function toShares(rows: CategoryAnswerDistribution[]): { category: string; A: number; B: number; C: number; D: number }[] {
  return rows.map((row) => {
    const total = row.total || 1;
    return {
      category: row.category,
      A: Math.round((row.a / total) * 100),
      B: Math.round((row.b / total) * 100),
      C: Math.round((row.c / total) * 100),
      D: Math.round((row.d / total) * 100),
    };
  });
}

function Metric({ label, value }: { label: string; value: number | string }): ReactElement {
  return (
    <Card>
      <CardHeader>
        <CardDescription>{label}</CardDescription>
        <CardTitle className="text-3xl tabular-nums text-primary">
          {typeof value === "number" ? value.toLocaleString() : value}
        </CardTitle>
      </CardHeader>
    </Card>
  );
}

function Breakdown({ title, header, rows }: { title: string; header: string; rows: NamedCount[] }): ReactElement {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {rows.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">No data yet.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{header}</TableHead>
                <TableHead className="w-24 text-right">Count</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.name}>
                  <TableCell className="capitalize">{row.name}</TableCell>
                  <TableCell className="text-right tabular-nums">{row.count.toLocaleString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}

function DashboardSkeleton(): ReactElement {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <Card key={index}>
            <CardHeader>
              <Skeleton className="h-4 w-24" />
              <Skeleton className="mt-2 h-8 w-20" />
            </CardHeader>
          </Card>
        ))}
      </div>
      <Skeleton className="h-72 w-full" />
    </div>
  );
}
