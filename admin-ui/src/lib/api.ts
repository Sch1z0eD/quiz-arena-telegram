const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export interface Me {
  id: number;
  name: string;
}

export interface Stats {
  questions: number;
  answers: number;
}

export interface NamedCount {
  name: string;
  count: number;
}

export interface DailyCount {
  day: string;
  count: number;
}

export interface Overview {
  players: { total: number; active7d: number; active30d: number };
  games: { solo: number; group: number; duel: number };
  questions: {
    active: number;
    inactive: number;
    byCategory: NamedCount[];
    byDifficulty: NamedCount[];
    byLanguage: NamedCount[];
  };
  categories: { active: number; hidden: number };
  answersPerDay: DailyCount[];
  topCategories: NamedCount[];
  accuracyPercent: number;
  totalAnswers: number;
}

export interface UserRow {
  id: number;
  name: string | null;
  username: string | null;
  language: string | null;
  games: number;
  accuracyPercent: number | null;
  elo: number;
  firstSeen: number;
  lastSeen: number;
  banned: boolean;
  blocked: boolean;
}

export interface UserDetail {
  summary: UserRow;
  categories: { category: string; answered: number; accuracyPercent: number | null }[];
  recentGames: { gameId: number; mode: string; finishedAt: number; correct: number; total: number }[];
  duel: { played: number; wins: number; draws: number; losses: number };
}

export interface UserQuery {
  q?: string;
  page?: number;
  size?: number;
}

export interface CategoryAnswerDistribution {
  category: string;
  a: number;
  b: number;
  c: number;
  d: number;
  total: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface QuestionSummary {
  id: number;
  text: string;
  category: string;
  difficulty: string;
  language: string;
  active: boolean;
}

export interface QuestionInput {
  text: string;
  options: string[];
  correctOption: number;
  category: string;
  difficulty: string;
  language: string;
}

export interface QuestionStats {
  answered: number;
  correct: number;
  accuracyPercent: number;
}

export interface QuestionDetail {
  id: number;
  text: string;
  options: string[];
  correctOption: number;
  category: string;
  difficulty: string;
  language: string;
  hash: string;
  active: boolean;
  stats: QuestionStats;
}

export interface CategoryRow {
  slug: string;
  names: Record<string, string>;
  active: boolean;
  questionCount: number;
  byLanguage: Record<string, number>;
}

export interface AuditEntry {
  id: number;
  ts: string;
  adminId: number;
  action: string;
  target: string | null;
  details: string | null;
}

export interface AuditQuery {
  action?: string;
  adminId?: string;
  target?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface QuestionQuery {
  q?: string;
  category?: string;
  difficulty?: string;
  language?: string;
  page?: number;
  size?: number;
}

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function csrfHeader(): Record<string, string> {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? { "X-XSRF-TOKEN": decodeURIComponent(match[1]) } : {};
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method ?? "GET";
  const headers: Record<string, string> = { ...(init.headers as Record<string, string> | undefined) };
  if (method !== "GET" && method !== "HEAD") {
    Object.assign(headers, csrfHeader());
  }
  const response = await fetch(`${API_BASE}/api/admin${path}`, {
    ...init,
    method,
    headers,
    credentials: "include",
  });
  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response, method, path));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function errorMessage(response: Response, method: string, path: string): Promise<string> {
  try {
    const body = (await response.json()) as { message?: unknown };
    if (typeof body.message === "string" && body.message) {
      return body.message;
    }
  } catch {
    // no JSON body; fall through to a generic message
  }
  return `${method} ${path} failed (${response.status})`;
}

function questionsQuery(query: QuestionQuery): string {
  const params = new URLSearchParams();
  if (query.q) {
    params.set("q", query.q);
  }
  if (query.category) {
    params.set("category", query.category);
  }
  if (query.difficulty) {
    params.set("difficulty", query.difficulty);
  }
  if (query.language) {
    params.set("language", query.language);
  }
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));
  return params.toString();
}

function auditQuery(query: AuditQuery): string {
  const params = new URLSearchParams();
  if (query.action) {
    params.set("action", query.action);
  }
  if (query.adminId) {
    params.set("adminId", query.adminId);
  }
  if (query.target) {
    params.set("target", query.target);
  }
  if (query.from) {
    params.set("from", query.from);
  }
  if (query.to) {
    params.set("to", query.to);
  }
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));
  return params.toString();
}

export const api = {
  me: (): Promise<Me> => request<Me>("/me"),
  stats: (): Promise<Stats> => request<Stats>("/stats"),
  overview: (): Promise<Overview> => request<Overview>("/stats/overview"),
  answerDistribution: (): Promise<CategoryAnswerDistribution[]> =>
    request<CategoryAnswerDistribution[]>("/stats/answer-distribution"),
  devLogin: (): Promise<void> => request<void>("/auth/dev-login"),
  logout: (): Promise<void> => request<void>("/auth/logout", { method: "POST" }),
  listQuestions: (query: QuestionQuery): Promise<PageResponse<QuestionSummary>> =>
    request<PageResponse<QuestionSummary>>(`/questions?${questionsQuery(query)}`),
  getQuestion: (id: number): Promise<QuestionDetail> => request<QuestionDetail>(`/questions/${id}`),
  createQuestion: (input: QuestionInput): Promise<QuestionDetail> =>
    request<QuestionDetail>("/questions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    }),
  updateQuestion: (id: number, input: QuestionInput): Promise<QuestionDetail> =>
    request<QuestionDetail>(`/questions/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    }),
  setQuestionActive: (id: number, active: boolean): Promise<QuestionDetail> =>
    request<QuestionDetail>(`/questions/${id}/active`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ active }),
    }),
  listAudit: (query: AuditQuery): Promise<PageResponse<AuditEntry>> =>
    request<PageResponse<AuditEntry>>(`/audit?${auditQuery(query)}`),
  listAuditActions: (): Promise<string[]> => request<string[]>("/audit/actions"),
  listUsers: (query: UserQuery): Promise<PageResponse<UserRow>> => {
    const params = new URLSearchParams();
    if (query.q) {
      params.set("q", query.q);
    }
    params.set("page", String(query.page ?? 0));
    params.set("size", String(query.size ?? 20));
    return request<PageResponse<UserRow>>(`/users?${params.toString()}`);
  },
  getUser: (id: number): Promise<UserDetail> => request<UserDetail>(`/users/${id}`),
  listCategories: (): Promise<CategoryRow[]> => request<CategoryRow[]>("/categories"),
  createCategory: (names: Record<string, string>, active: boolean): Promise<CategoryRow> =>
    request<CategoryRow>("/categories", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ names, active }),
    }),
  setCategoryActive: (slug: string, active: boolean): Promise<CategoryRow> =>
    request<CategoryRow>(`/categories/${encodeURIComponent(slug)}/active`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ active }),
    }),
  updateCategory: (slug: string, names: Record<string, string>): Promise<CategoryRow> =>
    request<CategoryRow>(`/categories/${encodeURIComponent(slug)}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ names }),
    }),
  deleteCategory: (slug: string): Promise<void> =>
    request<void>(`/categories/${encodeURIComponent(slug)}`, { method: "DELETE" }),
};
