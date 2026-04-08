import type { Category } from "../constants/categories";
import type { ReadHistoryItem } from "../types/article";
import type { CurrentUser, MyPageData } from "../types/member";
import { apiRequest } from "./http";

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

export function loginMember(payload: LoginPayload) {
  return apiRequest<CurrentUser>("/api/members/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function signupMember(payload: SignupPayload) {
  return apiRequest<CurrentUser>("/api/members/signup", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function logoutMember() {
  return apiRequest<void>("/api/members/logout", { method: "POST" });
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
