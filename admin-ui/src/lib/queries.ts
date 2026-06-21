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
  type CategoryAnswerDistribution,
  type CategoryRow,
  type Me,
  type Overview,
  type PageResponse,
  type QuestionDetail,
  type QuestionInput,
  type QuestionQuery,
  type QuestionSummary,
  type Stats,
} from "@/lib/api";

export function useMe(): UseQueryResult<Me> {
  return useQuery({ queryKey: ["me"], queryFn: api.me, retry: false });
}

export function useStats(): UseQueryResult<Stats> {
  return useQuery({ queryKey: ["stats"], queryFn: api.stats });
}

export function useOverview(): UseQueryResult<Overview> {
  return useQuery({ queryKey: ["overview"], queryFn: api.overview });
}

export function useAnswerDistribution(): UseQueryResult<CategoryAnswerDistribution[]> {
  return useQuery({ queryKey: ["answer-distribution"], queryFn: api.answerDistribution });
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
