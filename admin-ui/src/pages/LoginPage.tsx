import type { ReactElement } from "react";
import { Navigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useMe } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { TelegramLoginButton } from "@/components/TelegramLoginButton";

export function LoginPage(): ReactElement {
  const me = useMe();
  const queryClient = useQueryClient();

  if (me.isPending) {
    return <div className="flex min-h-screen items-center justify-center text-muted-foreground">Loading…</div>;
  }
  if (me.isSuccess) {
    return <Navigate to="/" replace />;
  }

  async function devLogin(): Promise<void> {
    await api.devLogin();
    await queryClient.invalidateQueries({ queryKey: ["me"] });
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl text-primary">QuizArena Admin</CardTitle>
          <CardDescription>Sign in with Telegram to continue.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <TelegramLoginButton />
          {import.meta.env.DEV && (
            <Button variant="outline" onClick={() => void devLogin()}>
              Dev login
            </Button>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
