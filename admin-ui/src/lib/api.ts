const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export interface Me {
  id: number;
  name: string;
}

export interface Stats {
  questions: number;
  answers: number;
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
    throw new ApiError(response.status, `${method} ${path} failed (${response.status})`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  me: (): Promise<Me> => request<Me>("/me"),
  stats: (): Promise<Stats> => request<Stats>("/stats"),
  devLogin: (): Promise<void> => request<void>("/auth/dev-login"),
  logout: (): Promise<void> => request<void>("/auth/logout", { method: "POST" }),
};
