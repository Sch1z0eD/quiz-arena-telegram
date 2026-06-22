import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";
import {
  api,
  type AuditEntry,
  type AuditQuery,
  type BroadcastMessage,
  type BroadcastSummary,
  type CategoryAnswerDistribution,
  type CategoryRow,
  type DryRunResult,
  type GameSettings,
  type Language,
  type Me,
  type Overview,
  type PageResponse,
  type PhotoUpload,
  type QuestionDetail,
  type QuestionInput,
  type QuestionQuery,
  type QuestionSummary,
  type UserDetail,
  type UserQuery,
  type UserRow,
} from "@/lib/api";

export function useMe(): UseQueryResult<Me> {
  return useQuery({ queryKey: ["me"], queryFn: api.me, retry: false });
}

export function useOverview(): UseQueryResult<Overview> {
  return useQuery({ queryKey: ["overview"], queryFn: api.overview });
}

export function useAnswerDistribution(): UseQueryResult<CategoryAnswerDistribution[]> {
  return useQuery({ queryKey: ["answer-distribution"], queryFn: api.answerDistribution });
}

export function useUsers(query: UserQuery): UseQueryResult<PageResponse<UserRow>> {
  return useQuery({
    queryKey: ["users", query],
    queryFn: () => api.listUsers(query),
    placeholderData: keepPreviousData,
  });
}

export function useUser(id: number | null): UseQueryResult<UserDetail> {
  return useQuery({
    queryKey: ["user", id],
    queryFn: () => api.getUser(id as number),
    enabled: id !== null,
    retry: false,
  });
}

export function useBroadcasts(page: number, size: number): UseQueryResult<PageResponse<BroadcastSummary>> {
  return useQuery({
    queryKey: ["broadcasts", page, size],
    queryFn: () => api.listBroadcasts(page, size),
    placeholderData: keepPreviousData,
    // Keep the history fresh while any broadcast is mid-flight so counters tick.
    refetchInterval: (query) =>
      (query.state.data?.content ?? []).some((b) => b.status === "RUNNING") ? 1500 : false,
  });
}

export function useBroadcast(id: number | null): UseQueryResult<BroadcastSummary> {
  return useQuery({
    queryKey: ["broadcast", id],
    queryFn: () => api.getBroadcast(id as number),
    enabled: id !== null,
    refetchInterval: (query) => (query.state.data?.status === "RUNNING" ? 1500 : false),
  });
}

export function useBroadcastTest(): UseMutationResult<BroadcastSummary, Error, BroadcastMessage> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (message) => api.broadcastTest(message),
    onSuccess: () => client.invalidateQueries({ queryKey: ["broadcasts"] }),
  });
}

export function useBroadcastDryRun(): UseMutationResult<DryRunResult, Error, { segment: string; language?: string; message: BroadcastMessage }> {
  return useMutation({ mutationFn: ({ segment, language, message }) => api.broadcastDryRun(segment, language, message) });
}

export function useBroadcastStart(): UseMutationResult<void, Error, { id: number; token: string }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, token }) => api.broadcastStart(id, token),
    onSuccess: () => client.invalidateQueries({ queryKey: ["broadcasts"] }),
  });
}

export function useBroadcastAbort(): UseMutationResult<void, Error, number> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id) => api.broadcastAbort(id),
    onSuccess: () => client.invalidateQueries({ queryKey: ["broadcasts"] }),
  });
}

export function useBroadcastPhotoUpload(): UseMutationResult<PhotoUpload, Error, File> {
  return useMutation({ mutationFn: (file) => api.uploadBroadcastPhoto(file) });
}

export function useLanguages(): UseQueryResult<Language[]> {
  return useQuery({ queryKey: ["languages"], queryFn: api.listLanguages, staleTime: 60 * 60 * 1000 });
}

export function useGameSettings(): UseQueryResult<GameSettings> {
  return useQuery({ queryKey: ["settings"], queryFn: api.getSettings });
}

export function useUpdateGameSettings(): UseMutationResult<GameSettings, Error, GameSettings> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (settings) => api.updateSettings(settings),
    onSuccess: (data) => client.setQueryData(["settings"], data),
  });
}

export function useSetUserBanned(): UseMutationResult<void, Error, { id: number; banned: boolean }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, banned }) => api.setUserBanned(id, banned),
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["users"] }),
        client.invalidateQueries({ queryKey: ["user"] }),
      ]);
    },
  });
}

export function useQuestions(query: QuestionQuery): UseQueryResult<PageResponse<QuestionSummary>> {
  return useQuery({
    queryKey: ["questions", query],
    queryFn: () => api.listQuestions(query),
    placeholderData: keepPreviousData,
  });
}

export function useQuestion(id: number): UseQueryResult<QuestionDetail> {
  return useQuery({ queryKey: ["question", id], queryFn: () => api.getQuestion(id), retry: false });
}

function useQuestionInvalidation(): () => Promise<void> {
  const client = useQueryClient();
  return async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["questions"] }),
      client.invalidateQueries({ queryKey: ["question"] }),
      client.invalidateQueries({ queryKey: ["categories"] }),
    ]);
  };
}

export function useCreateQuestion(): UseMutationResult<QuestionDetail, Error, QuestionInput> {
  const invalidate = useQuestionInvalidation();
  return useMutation({ mutationFn: (input) => api.createQuestion(input), onSuccess: invalidate });
}

export function useUpdateQuestion(): UseMutationResult<QuestionDetail, Error, { id: number; input: QuestionInput }> {
  const invalidate = useQuestionInvalidation();
  return useMutation({ mutationFn: ({ id, input }) => api.updateQuestion(id, input), onSuccess: invalidate });
}

export function useSetQuestionActive(): UseMutationResult<QuestionDetail, Error, { id: number; active: boolean }> {
  const invalidate = useQuestionInvalidation();
  return useMutation({ mutationFn: ({ id, active }) => api.setQuestionActive(id, active), onSuccess: invalidate });
}

export function useCategories(): UseQueryResult<CategoryRow[]> {
  return useQuery({ queryKey: ["categories"], queryFn: api.listCategories });
}

export function useAudit(query: AuditQuery): UseQueryResult<PageResponse<AuditEntry>> {
  return useQuery({
    queryKey: ["audit", query],
    queryFn: () => api.listAudit(query),
    placeholderData: keepPreviousData,
  });
}

export function useAuditActions(): UseQueryResult<string[]> {
  return useQuery({ queryKey: ["audit-actions"], queryFn: api.listAuditActions });
}

function useCategoryInvalidation(): () => Promise<void> {
  const client = useQueryClient();
  return async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["categories"] }),
      client.invalidateQueries({ queryKey: ["questions"] }),
    ]);
  };
}

export function useCreateCategory(): UseMutationResult<CategoryRow, Error, { names: Record<string, string>; active: boolean }> {
  const invalidate = useCategoryInvalidation();
  return useMutation({ mutationFn: ({ names, active }) => api.createCategory(names, active), onSuccess: invalidate });
}

export function useSetCategoryActive(): UseMutationResult<CategoryRow, Error, { slug: string; active: boolean }> {
  const invalidate = useCategoryInvalidation();
  return useMutation({ mutationFn: ({ slug, active }) => api.setCategoryActive(slug, active), onSuccess: invalidate });
}

export function useUpdateCategory(): UseMutationResult<CategoryRow, Error, { slug: string; names: Record<string, string> }> {
  const invalidate = useCategoryInvalidation();
  return useMutation({ mutationFn: ({ slug, names }) => api.updateCategory(slug, names), onSuccess: invalidate });
}

export function useDeleteCategory(): UseMutationResult<void, Error, string> {
  const invalidate = useCategoryInvalidation();
  return useMutation({ mutationFn: (slug) => api.deleteCategory(slug), onSuccess: invalidate });
}
