import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { CategoryRow } from "@/lib/api";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

export function categoryLabel(category: CategoryRow): string {
  return category.names.en ?? category.names.ru ?? category.slug;
}

export function formatTs(ms: number): string {
  return new Date(ms).toLocaleString();
}

export function toOptions(values: string[]): { value: string; label: string }[] {
  return values.map((value) => ({ value, label: value }));
}
