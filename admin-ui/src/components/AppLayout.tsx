import type { ReactElement } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { LayoutDashboard, ListChecks, LogOut, ScrollText, Tags, Users } from "lucide-react";
import { api } from "@/lib/api";
import { useMe } from "@/lib/queries";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/questions", label: "Questions", icon: ListChecks, end: false },
  { to: "/categories", label: "Categories", icon: Tags, end: false },
  { to: "/users", label: "Users", icon: Users, end: false },
  { to: "/audit", label: "Audit", icon: ScrollText, end: false },
];

export function AppLayout(): ReactElement {
  const me = useMe();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  async function logout(): Promise<void> {
    await api.logout();
    queryClient.clear();
    navigate("/login", { replace: true });
  }

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 shrink-0 flex-col border-r bg-card/40 p-4">
        <div className="px-2 pb-6 text-sm font-semibold tracking-wide text-primary">QuizArena Admin</div>
        <nav className="flex flex-1 flex-col gap-1">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground",
                  isActive && "bg-muted text-foreground",
                )
              }
            >
              <item.icon className="size-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-4 border-t pt-4">
          <div className="px-3 pb-2 text-xs text-muted-foreground">{me.data?.name ?? ""}</div>
          <Button
            variant="ghost"
            className="w-full justify-start gap-3 text-muted-foreground"
            onClick={() => void logout()}
          >
            <LogOut className="size-4" />
            Log out
          </Button>
        </div>
      </aside>
      <main className="flex-1 overflow-x-hidden">
        <div className="mx-auto max-w-5xl p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
