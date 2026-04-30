const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

const ACCESS_TOKEN_KEY = "jwt_token";

interface ApiErrorPayload {
  code?: string;
  message?: string;
}

export class ApiError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function isAuthenticationError(error: unknown): boolean {
  return isApiError(error) && (error.status === 401 || error.code === "UNAUTHORIZED");
}

export function getStoredToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function clearStoredToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
}

/**
 * refresh 쿠키(httpOnly)를 이용해 새 access token을 받는다.
 * refresh token은 브라우저가 쿠키로 자동 전송하므로 JS에서 직접 다루지 않는다.
 */
async function refreshAccessToken(): Promise<string | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/members/refresh`, {
      method: "POST",
      credentials: "same-origin",  // httpOnly 쿠키 전송을 위해 필수
      headers: { "Content-Type": "application/json" },
    });

    if (!response.ok) {
      clearStoredToken();
      return null;
    }

    const data = (await response.json()) as { token: string };
    setStoredToken(data.token);
    return data.token;
  } catch {
    clearStoredToken();
    return null;
  }
}

// 동시에 여러 요청이 401을 받아도 refresh 호출은 1번만 실행되도록 in-flight Promise를 공유한다.
// 그렇지 않으면 후속 요청이 이미 폐기된(rotation된) refresh token으로 재요청 → 백엔드가 재사용으로 판단해
// 회원 전체 토큰을 일괄 폐기하고 사용자가 강제 로그아웃되는 문제가 생긴다.
let refreshPromise: Promise<string | null> | null = null;

function getFreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetchWithAuth(path, init);

  // 401이면 refresh 쿠키로 자동 갱신 시도
  if (response.status === 401) {
    const newToken = await getFreshAccessToken();
    if (newToken) {
      const retryResponse = await fetchWithAuth(path, init);
      return handleResponse<T>(retryResponse);
    }
  }

  return handleResponse<T>(response);
}

async function fetchWithAuth(path: string, init?: RequestInit): Promise<Response> {
  const token = getStoredToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init?.headers as Record<string, string> ?? {}),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "same-origin",  // httpOnly 쿠키 전송을 위해 필수
    headers,
  });
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as ApiErrorPayload | null;
    throw new ApiError(payload?.message ?? "오류가 발생했습니다.", response.status, payload?.code);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
