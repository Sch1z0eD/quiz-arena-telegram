import type { ReactElement } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useMe } from "@/lib/queries";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";

function RequireAuth({ children }: { children: ReactElement }): ReactElement {
  const me = useMe();
  if (me.isPending) {
    return <div className="flex min-h-screen items-center justify-center text-muted-foreground">Loading…</div>;
  }
  if (me.isError) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App(): ReactElement {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
