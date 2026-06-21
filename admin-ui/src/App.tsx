import type { ReactElement } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useMe } from "@/lib/queries";
import { AppLayout } from "@/components/AppLayout";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { QuestionsPage } from "@/pages/QuestionsPage";
import { QuestionDetailPage } from "@/pages/QuestionDetailPage";
import { CategoriesPage } from "@/pages/CategoriesPage";
import { AuditPage } from "@/pages/AuditPage";

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
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/questions" element={<QuestionsPage />} />
        <Route path="/questions/:id" element={<QuestionDetailPage />} />
        <Route path="/categories" element={<CategoriesPage />} />
        <Route path="/audit" element={<AuditPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
