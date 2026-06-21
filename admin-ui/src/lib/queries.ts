import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { api, type Me, type Stats } from "@/lib/api";

export function useMe(): UseQueryResult<Me> {
  return useQuery({ queryKey: ["me"], queryFn: api.me, retry: false });
}

export function useStats(): UseQueryResult<Stats> {
  return useQuery({ queryKey: ["stats"], queryFn: api.stats });
}
