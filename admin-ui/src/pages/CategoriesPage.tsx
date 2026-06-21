import { useState, type ReactElement } from "react";
import { Pencil, Plus, Power, Trash2 } from "lucide-react";
import { ApiError, type CategoryRow } from "@/lib/api";
import { useCategories, useCreateCategory, useDeleteCategory, useSetCategoryActive, useUpdateCategory } from "@/lib/queries";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

const LANGUAGES: { code: string; label: string }[] = [
  { code: "ru", label: "Russian" },
  { code: "en", label: "English" },
];

export function CategoriesPage(): ReactElement {
  const categories = useCategories();
  const toggle = useSetCategoryActive();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<CategoryRow | null>(null);
  const [deleting, setDeleting] = useState<CategoryRow | null>(null);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Categories</h1>
        <Button size="sm" onClick={() => setCreating(true)}>
          <Plus className="size-4" /> New category
        </Button>
      </div>

      {categories.isError ? (
        <div className="flex flex-col items-center gap-3 rounded-md border border-dashed py-12 text-sm text-muted-foreground">
          <span>Failed to load categories.</span>
          <Button variant="outline" size="sm" onClick={() => void categories.refetch()}>Retry</Button>
        </div>
      ) : categories.isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : categories.data.length === 0 ? (
        <p className="py-12 text-center text-sm text-muted-foreground">No categories yet.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Category</TableHead>
              {LANGUAGES.map((language) => (
                <TableHead key={language.code} className="w-24 text-right uppercase">{language.code}</TableHead>
              ))}
              <TableHead className="w-24 text-right">Total</TableHead>
              <TableHead className="w-24" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {categories.data.map((row) => (
              <TableRow key={row.slug} className={cn(!row.active && "opacity-50")}>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{row.names.en ?? row.slug}</span>
                    {!row.active ? <Badge variant="outline">Disabled</Badge> : null}
                  </div>
                  <div className="text-xs text-muted-foreground">{row.slug}</div>
                </TableCell>
                {LANGUAGES.map((language) => (
                  <TableCell key={language.code} className="text-right tabular-nums text-muted-foreground">
                    {(row.byLanguage[language.code] ?? 0).toLocaleString()}
                  </TableCell>
                ))}
                <TableCell className="text-right font-medium tabular-nums">{row.questionCount.toLocaleString()}</TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" disabled={toggle.isPending}
                      aria-label={`${row.active ? "Disable" : "Enable"} ${row.slug}`}
                      onClick={() => toggle.mutate({ slug: row.slug, active: !row.active })}>
                      <Power className={cn("size-4", row.active ? "text-primary" : "text-muted-foreground")} />
                    </Button>
                    <Button variant="ghost" size="icon" aria-label={`Edit ${row.slug}`} onClick={() => setEditing(row)}>
                      <Pencil className="size-4" />
                    </Button>
                    <Button variant="ghost" size="icon" aria-label={`Delete ${row.slug}`} onClick={() => setDeleting(row)}>
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {creating ? <CreateCategoryDialog onClose={() => setCreating(false)} /> : null}
      {editing ? <EditCategoryDialog key={editing.slug} row={editing} onClose={() => setEditing(null)} /> : null}
      {deleting ? <DeleteCategoryDialog row={deleting} onClose={() => setDeleting(null)} /> : null}
    </div>
  );
}

function NameFields({ names, onChange, disabled }: {
  names: Record<string, string>;
  onChange: (code: string, value: string) => void;
  disabled: boolean;
}): ReactElement {
  return (
    <>
      {LANGUAGES.map((language) => (
        <div key={language.code} className="flex flex-col gap-1.5">
          <Label htmlFor={`name-${language.code}`}>{language.label}</Label>
          <Input
            id={`name-${language.code}`}
            value={names[language.code] ?? ""}
            disabled={disabled}
            onChange={(event) => onChange(language.code, event.target.value)}
          />
        </div>
      ))}
    </>
  );
}

function CreateCategoryDialog({ onClose }: { onClose: () => void }): ReactElement {
  const create = useCreateCategory();
  const [names, setNames] = useState<Record<string, string>>({});
  const [active, setActive] = useState(false);
  const complete = LANGUAGES.every((language) => (names[language.code] ?? "").trim().length > 0);

  function submit(): void {
    create.mutate({ names, active }, { onSuccess: onClose });
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New category</DialogTitle>
          <DialogDescription>The slug is generated from the English name.</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <NameFields
            names={names}
            disabled={create.isPending}
            onChange={(code, value) => setNames((prev) => ({ ...prev, [code]: value }))}
          />
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="size-4 accent-primary"
              checked={active}
              disabled={create.isPending}
              onChange={(event) => setActive(event.target.checked)}
            />
            Active (visible in the bot once it has enough questions)
          </label>
        </div>
        {create.isError ? <p className="text-sm text-destructive">{create.error.message}</p> : null}
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={create.isPending}>Cancel</Button>
          <Button onClick={submit} disabled={!complete || create.isPending}>
            {create.isPending ? "Creating…" : "Create"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function EditCategoryDialog({ row, onClose }: { row: CategoryRow; onClose: () => void }): ReactElement {
  const update = useUpdateCategory();
  const [names, setNames] = useState<Record<string, string>>(() =>
    Object.fromEntries(LANGUAGES.map((language) => [language.code, row.names[language.code] ?? ""])),
  );
  const complete = LANGUAGES.every((language) => (names[language.code] ?? "").trim().length > 0);

  function submit(): void {
    update.mutate({ slug: row.slug, names }, { onSuccess: onClose });
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit category</DialogTitle>
          <DialogDescription>Slug <span className="font-mono">{row.slug}</span> cannot be changed.</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <NameFields
            names={names}
            disabled={update.isPending}
            onChange={(code, value) => setNames((prev) => ({ ...prev, [code]: value }))}
          />
        </div>
        {update.isError ? <p className="text-sm text-destructive">{update.error.message}</p> : null}
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={update.isPending}>Cancel</Button>
          <Button onClick={submit} disabled={!complete || update.isPending}>
            {update.isPending ? "Saving…" : "Save"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DeleteCategoryDialog({ row, onClose }: { row: CategoryRow; onClose: () => void }): ReactElement {
  const remove = useDeleteCategory();
  const conflictMessage =
    remove.error instanceof ApiError && remove.error.status === 409 ? remove.error.message : null;

  function confirm(): void {
    remove.mutate(row.slug, { onSuccess: onClose });
  }

  return (
    <AlertDialog open onOpenChange={(open) => { if (!open) onClose(); }}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete {row.names.en ?? row.slug}?</AlertDialogTitle>
          <AlertDialogDescription>
            {conflictMessage ?? "This category will be removed permanently."}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={remove.isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            className="bg-destructive text-destructive-foreground hover:opacity-90"
            disabled={remove.isPending || conflictMessage !== null}
            onClick={(event) => { event.preventDefault(); confirm(); }}
          >
            {remove.isPending ? "Deleting…" : "Delete"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
