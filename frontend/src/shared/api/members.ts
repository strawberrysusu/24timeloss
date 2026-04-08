import type { Category } from "../constants/categories";
import type { ReadHistoryItem } from "../types/article";
import type { CurrentUser, LoginResponseData, MyPageData } from "../types/member";
import { apiRequest, clearStoredToken, setStoredToken } from "./http";

interface LoginPayload {
  email: string;
  password: string;
}

interface SignupPayload extends LoginPayload {
  nickname: string;
}

export function getCurrentUser() {
  return apiRequest<CurrentUser>("/api/members/me");
}

export async function loginMember(payload: LoginPayload): Promise<CurrentUser> {
  const data = await apiRequest<LoginResponseData>("/api/members/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  setStoredToken(data.token);
  return { id: data.id, email: data.email, nickname: data.nickname };
}

export async function signupMember(payload: SignupPayload): Promise<CurrentUser> {
  const data = await apiRequest<LoginResponseData>("/api/members/signup", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  setStoredToken(data.token);
  return { id: data.id, email: data.email, nickname: data.nickname };
}

export async function logoutMember(): Promise<void> {
  // 서버에서 httpOnly refresh 쿠키를 삭제한다
  await apiRequest<void>("/api/members/logout", { method: "POST" }).catch(() => undefined);
  clearStoredToken();
}

export function getMyPage() {
  return apiRequest<MyPageData>("/api/mypage");
}

export function listSavedArticleIds() {
  return apiRequest<number[]>("/api/mypage/saved-article-ids");
}

export function saveArticle(articleId: number) {
  return apiRequest<void>(`/api/mypage/saved-articles/${articleId}`, { method: "POST" });
}

export function unsaveArticle(articleId: number) {
  return apiRequest<void>(`/api/mypage/saved-articles/${articleId}`, { method: "DELETE" });
}

export function addInterest(category: Category) {
  return apiRequest<void>(`/api/mypage/interests/${category}`, { method: "POST" });
}

export function removeInterest(category: Category) {
  return apiRequest<void>(`/api/mypage/interests/${category}`, { method: "DELETE" });
}

export function getReadHistory() {
  return apiRequest<ReadHistoryItem[]>("/api/mypage/read-history");
}

export function recordReadHistory(articleId: number) {
  return apiRequest<void>(`/api/mypage/read-history/${articleId}`, { method: "POST" });
}

export function updateNickname(nickname: string) {
  return apiRequest<CurrentUser>("/api/mypage/nickname", {
    method: "PATCH",
    body: JSON.stringify({ nickname }),
  });
}

export function updatePassword(currentPassword: string, newPassword: string) {
  return apiRequest<void>("/api/mypage/password", {
    method: "PATCH",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}
