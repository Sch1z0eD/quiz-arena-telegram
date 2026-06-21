import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { CategoryRow } from "@/lib/api";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

export function categoryLabel(category: CategoryRow): string {
  return category.names.en ?? category.names.ru ?? category.slug;
}
