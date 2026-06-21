import type { ReactElement } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps): ReactElement {
  return (
    <div className="flex items-center justify-between pt-4 text-sm text-muted-foreground">
      <span className="tabular-nums">{totalElements.toLocaleString()} total</span>
      <div className="flex items-center gap-3">
        <span className="tabular-nums">Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</span>
        <Button variant="outline" size="icon" aria-label="Previous page" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft className="size-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          aria-label="Next page"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  );
}
