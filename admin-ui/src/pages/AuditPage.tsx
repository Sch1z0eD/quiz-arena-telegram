import { Fragment, useState, type ReactElement } from "react";
import { useSearchParams } from "react-router-dom";
import type { AuditQuery } from "@/lib/api";
import { useAudit, useAuditActions } from "@/lib/queries";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/Pagination";

const ALL = "all";
const PAGE_SIZE = 20;

export function AuditPage(): ReactElement {
  const [params, setParams] = useSearchParams();
  const actions = useAuditActions();
  const [expanded, setExpanded] = useState<number | null>(null);

  const fromDate = params.get("from") ?? "";
  const toDate = params.get("to") ?? "";
  const query: AuditQuery = {
    action: params.get("action") ?? "",
    adminId: params.get("adminId") ?? "",
    target: params.get("target") ?? "",
    from: fromDate ? `${fromDate}T00:00:00.000Z` : "",
    to: toDate ? `${toDate}T23:59:59.999Z` : "",
    page: Number(params.get("page") ?? "0"),
    size: PAGE_SIZE,
  };
  const audit = useAudit(query);

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
      <h1 className="mb-6 text-xl font-semibold">Audit log</h1>

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <Label>Action</Label>
          <Select value={(params.get("action") ?? "") || ALL}
            onValueChange={(next) => setParam("action", next === ALL ? "" : next)}>
            <SelectTrigger className="w-52"><SelectValue placeholder="Action" /></SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All actions</SelectItem>
              {(actions.data ?? []).map((action) => (
                <SelectItem key={action} value={action}>{action}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Field label="Admin ID">
          <Input className="w-32" inputMode="numeric" value={params.get("adminId") ?? ""}
            onChange={(event) => setParam("adminId", event.target.value.replace(/[^0-9]/g, ""))} />
        </Field>
        <Field label="From">
          <Input type="date" className="w-40" value={fromDate} onChange={(event) => setParam("from", event.target.value)} />
        </Field>
        <Field label="To">
          <Input type="date" className="w-40" value={toDate} onChange={(event) => setParam("to", event.target.value)} />
        </Field>
        <Field label="Target">
          <Input className="w-48" placeholder="Search target…" value={params.get("target") ?? ""}
            onChange={(event) => setParam("target", event.target.value)} />
        </Field>
      </div>

      {audit.isError ? (
        <div className="flex flex-col items-center gap-3 rounded-md border border-dashed py-12 text-sm text-muted-foreground">
          <span>Failed to load the audit log.</span>
          <Button variant="outline" size="sm" onClick={() => void audit.refetch()}>Retry</Button>
        </div>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-48">Time</TableHead>
                <TableHead className="w-32 text-right">Admin</TableHead>
                <TableHead className="w-56">Action</TableHead>
                <TableHead>Target</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {audit.isPending ? (
                <SkeletonRows />
              ) : audit.data.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="py-12 text-center text-muted-foreground">
                    No audit entries match your filters.
                  </TableCell>
                </TableRow>
              ) : (
                audit.data.content.map((entry) => (
                  <Fragment key={entry.id}>
                    <TableRow className="cursor-pointer"
                      onClick={() => setExpanded((current) => (current === entry.id ? null : entry.id))}>
                      <TableCell className="tabular-nums text-muted-foreground">{new Date(entry.ts).toLocaleString()}</TableCell>
                      <TableCell className="text-right tabular-nums">{entry.adminId}</TableCell>
                      <TableCell className="font-medium">{entry.action}</TableCell>
                      <TableCell className="text-muted-foreground">{entry.target ?? "—"}</TableCell>
                    </TableRow>
                    {expanded === entry.id ? (
                      <TableRow>
                        <TableCell colSpan={4} className="bg-muted/30">
                          <pre className="whitespace-pre-wrap break-words font-mono text-xs text-muted-foreground">
                            {entry.details ?? "No details."}
                          </pre>
                        </TableCell>
                      </TableRow>
                    ) : null}
                  </Fragment>
                ))
              )}
            </TableBody>
          </Table>
          {audit.data ? (
            <Pagination
              page={query.page ?? 0}
              totalPages={audit.data.totalPages}
              totalElements={audit.data.totalElements}
              onPageChange={(page) => setParam("page", String(page))}
            />
          ) : null}
        </>
      )}
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactElement }): ReactElement {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}

function SkeletonRows(): ReactElement {
  return (
    <>
      {Array.from({ length: 8 }).map((_, index) => (
        <TableRow key={index}>
          <TableCell><Skeleton className="h-4 w-36" /></TableCell>
          <TableCell><Skeleton className="ml-auto h-4 w-16" /></TableCell>
          <TableCell><Skeleton className="h-4 w-40" /></TableCell>
          <TableCell><Skeleton className="h-4 w-24" /></TableCell>
        </TableRow>
      ))}
    </>
  );
}
