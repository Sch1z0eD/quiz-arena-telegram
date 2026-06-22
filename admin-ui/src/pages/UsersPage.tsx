import { useState, type ReactElement } from "react";
import { useSearchParams } from "react-router-dom";
import type { UserQuery } from "@/lib/api";
import { useSetUserBanned, useUser, useUsers } from "@/lib/queries";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/Pagination";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

function formatTs(ms: number): string {
  return new Date(ms).toLocaleString();
}

function pct(value: number | null): string {
  return value === null ? "—" : `${value}%`;
}

export function UsersPage(): ReactElement {
  const [params, setParams] = useSearchParams();
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const query: UserQuery = {
    q: params.get("q") ?? "",
    page: Number(params.get("page") ?? "0"),
    size: PAGE_SIZE,
  };
  const usersList = useUsers(query);

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
      <h1 className="mb-6 text-xl font-semibold">Users</h1>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Input
          className="max-w-xs"
          placeholder="Search by id or name…"
          aria-label="Search users"
          value={query.q}
          onChange={(event) => setParam("q", event.target.value)}
        />
      </div>

      {usersList.isError ? (
        <div className="flex flex-col items-center gap-3 rounded-md border border-dashed py-12 text-sm text-muted-foreground">
          <span>Failed to load users.</span>
          <Button variant="outline" size="sm" onClick={() => void usersList.refetch()}>Retry</Button>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-28 text-right">ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Username</TableHead>
                <TableHead className="w-16">Lang</TableHead>
                <TableHead className="w-20 text-right" title="Solo and group quiz games (duels shown in detail)">Games</TableHead>
                <TableHead className="w-24 text-right" title="Across all modes, including duels">Accuracy</TableHead>
                <TableHead className="w-20 text-right">ELO</TableHead>
                <TableHead className="w-40">Last seen</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {usersList.isPending ? (
                <SkeletonRows />
              ) : usersList.data.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="py-12 text-center text-muted-foreground">No users match your search.</TableCell>
                </TableRow>
              ) : (
                usersList.data.content.map((user) => (
                  <TableRow key={user.id} className="cursor-pointer" onClick={() => setSelectedId(user.id)}>
                    <TableCell className="text-right tabular-nums text-muted-foreground">{user.id}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <span>{user.name ?? "—"}</span>
                        {user.banned ? <Badge variant="outline">Banned</Badge> : null}
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{user.username ? `@${user.username}` : "—"}</TableCell>
                    <TableCell className="uppercase text-muted-foreground">{user.language ?? "—"}</TableCell>
                    <TableCell className="text-right tabular-nums">{user.games.toLocaleString()}</TableCell>
                    <TableCell className="text-right tabular-nums">{pct(user.accuracyPercent)}</TableCell>
                    <TableCell className="text-right tabular-nums">{user.elo}</TableCell>
                    <TableCell className="text-muted-foreground">{formatTs(user.lastSeen)}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          {usersList.data ? (
            <Pagination
              page={query.page ?? 0}
              totalPages={usersList.data.totalPages}
              totalElements={usersList.data.totalElements}
              onPageChange={(page) => setParam("page", String(page))}
            />
          ) : null}
        </>
      )}

      <Sheet open={selectedId !== null} onOpenChange={(open) => { if (!open) setSelectedId(null); }}>
        <SheetContent>
          {selectedId !== null ? <UserDetailPanel id={selectedId} /> : null}
        </SheetContent>
      </Sheet>
    </div>
  );
}

function UserDetailPanel({ id }: { id: number }): ReactElement {
  const detail = useUser(id);
  const ban = useSetUserBanned();
  const [confirming, setConfirming] = useState(false);

  if (detail.isPending) {
    return <Skeleton className="h-64 w-full" />;
  }
  if (detail.isError || !detail.data) {
    return <p className="text-sm text-destructive">Failed to load user.</p>;
  }

  const u = detail.data.summary;
  return (
    <>
      <SheetHeader>
        <div className="flex items-center gap-2">
          <SheetTitle>{u.name ?? `User ${u.id}`}</SheetTitle>
          {u.banned ? <Badge variant="outline">Banned</Badge> : null}
        </div>
        <SheetDescription>
          {u.username ? `@${u.username} · ` : ""}id {u.id}{u.language ? ` · ${u.language.toUpperCase()}` : ""}
        </SheetDescription>
      </SheetHeader>

      <div>
        {confirming ? (
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">
              {u.banned ? "Unban — the bot will respond again." : "Ban — the bot will silently ignore this user."}
            </span>
            <Button variant="outline" size="sm" disabled={ban.isPending} onClick={() => setConfirming(false)}>Cancel</Button>
            <Button
              size="sm"
              className={u.banned ? "" : "bg-destructive text-destructive-foreground hover:opacity-90"}
              disabled={ban.isPending}
              onClick={() => ban.mutate({ id: u.id, banned: !u.banned }, { onSuccess: () => setConfirming(false) })}
            >
              {ban.isPending ? "Saving…" : u.banned ? "Confirm unban" : "Confirm ban"}
            </Button>
          </div>
        ) : (
          <Button variant={u.banned ? "outline" : "destructive"} size="sm" onClick={() => setConfirming(true)}>
            {u.banned ? "Unban" : "Ban"}
          </Button>
        )}
        {ban.isError ? <p className="mt-2 text-sm text-destructive">{ban.error.message}</p> : null}
      </div>

      <div className="grid grid-cols-2 gap-3 text-sm">
        <Stat label="Games (solo/group)" value={u.games.toLocaleString()} />
        <Stat label="Accuracy (all modes)" value={pct(u.accuracyPercent)} />
        <Stat label="ELO" value={String(u.elo)} />
        <Stat label="Duels" value={`${detail.data.duel.played}`} />
        <Stat label="First seen" value={formatTs(u.firstSeen)} />
        <Stat label="Last seen" value={formatTs(u.lastSeen)} />
      </div>

      <Section title="Duel record">
        <p className="text-sm text-muted-foreground">
          {detail.data.duel.played} played · {detail.data.duel.wins}W / {detail.data.duel.draws}D / {detail.data.duel.losses}L
        </p>
      </Section>

      <Section title="By category">
        {detail.data.categories.length === 0 ? (
          <Empty />
        ) : (
          <MiniTable
            head={["Category", "Answered", "Accuracy"]}
            rows={detail.data.categories.map((c) => [c.category, c.answered.toLocaleString(), pct(c.accuracyPercent)])}
          />
        )}
      </Section>

      <Section title="Recent games">
        {detail.data.recentGames.length === 0 ? (
          <Empty />
        ) : (
          <MiniTable
            head={["Mode", "Score", "When"]}
            rows={detail.data.recentGames.map((g) => [
              g.mode,
              `${g.correct}/${g.total}`,
              formatTs(g.finishedAt),
            ])}
          />
        )}
      </Section>
    </>
  );
}

function Stat({ label, value }: { label: string; value: string }): ReactElement {
  return (
    <div className="rounded-md border p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="tabular-nums">{value}</div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactElement }): ReactElement {
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-sm font-medium">{title}</h3>
      {children}
    </div>
  );
}

function MiniTable({ head, rows }: { head: string[]; rows: string[][] }): ReactElement {
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="text-left text-xs text-muted-foreground">
          {head.map((h) => <th key={h} className="pb-1 font-normal">{h}</th>)}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, index) => (
          <tr key={index} className="border-t border-border/50">
            {row.map((cell, cellIndex) => (
              <td key={cellIndex} className={cn("py-1.5", cellIndex === 0 && "capitalize")}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function Empty(): ReactElement {
  return <p className="text-sm text-muted-foreground">No data.</p>;
}

function SkeletonRows(): ReactElement {
  return (
    <>
      {Array.from({ length: 8 }).map((_, index) => (
        <TableRow key={index}>
          {Array.from({ length: 8 }).map((__, cell) => (
            <TableCell key={cell}><Skeleton className="h-4 w-full" /></TableCell>
          ))}
        </TableRow>
      ))}
    </>
  );
}
