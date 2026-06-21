import type { ReactElement } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { useMe, useStats } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function DashboardPage(): ReactElement {
  const me = useMe();
  const stats = useStats();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  async function logout(): Promise<void> {
    await api.logout();
    queryClient.clear();
    navigate("/login", { replace: true });
  }

  return (
    <div className="mx-auto max-w-3xl p-6">
      <header className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary">QuizArena Admin</h1>
          <p className="text-muted-foreground">
            {me.data ? `Signed in as ${me.data.name}` : "…"}
          </p>
        </div>
        <Button variant="outline" onClick={() => void logout()}>
          Log out
        </Button>
      </header>
      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard title="Questions" value={stats.data?.questions} />
        <StatCard title="Answers" value={stats.data?.answers} />
      </div>
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: number | undefined }): ReactElement {
  return (
    <Card>
      <CardHeader>
        <CardDescription>{title}</CardDescription>
        <CardTitle className="text-4xl text-primary">
          {value === undefined ? "—" : value.toLocaleString()}
        </CardTitle>
      </CardHeader>
    </Card>
  );
}
