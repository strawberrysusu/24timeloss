import type { Category } from "../constants/categories";
import type {
  ArticleCardData,
  ArticleDetailData,
  ArticleEditorValues,
  ArticleExtractResult,
} from "../types/article";
import type { PageResponse } from "../types/page";
import { apiRequest } from "./http";

interface BriefingResponse {
  text: string;
  generatedAt: string;
}

interface GetArticlesParams {
  page?: number;
  size?: number;
  category?: Category | null;
}

interface SearchArticlesParams {
  keyword: string;
  page?: number;
  size?: number;
}

function normalizeRequired(value: string) {
  return value.trim();
}

function normalizeOptional(value: string, emptyValue: "" | null) {
  const trimmed = value.trim();
  return trimmed === "" ? emptyValue : trimmed;
}

function buildQuery(params: Record<string, string | number | null | undefined>) {
  const query = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === "") {
      continue;
    }
    query.set(key, String(value));
  }

  const queryString = query.toString();
  return queryString ? `?${queryString}` : "";
}

export function getArticles({
  page = 0,
  size = 10,
  category = null,
}: GetArticlesParams = {}) {
  return apiRequest<PageResponse<ArticleCardData>>(
    `/api/articles${buildQuery({ page, size, category })}`,
  );
}

export function searchArticleList({
  keyword,
  page = 0,
  size = 10,
}: SearchArticlesParams) {
  return apiRequest<PageResponse<ArticleCardData>>(
    `/api/articles/search${buildQuery({ keyword, page, size })}`,
  );
}

export function getTrendingArticles() {
  return apiRequest<ArticleCardData[]>("/api/articles/trending");
}

export function getPopularKeywords() {
  return apiRequest<string[]>("/api/articles/popular-keywords");
}

export function getBriefing() {
  return apiRequest<BriefingResponse>("/api/articles/briefing");
}

export function getArticleDetail(articleId: number) {
  return apiRequest<ArticleDetailData>(`/api/articles/${articleId}`);
}

export function getRelatedArticles(articleId: number) {
  return apiRequest<ArticleCardData[]>(`/api/articles/${articleId}/related`);
}

export function generateArticleSummary(articleId: number) {
  return apiRequest<ArticleDetailData>(`/api/articles/${articleId}/generate-summary`, {
    method: "POST",
  });
}

export function deleteArticle(articleId: number) {
  return apiRequest<void>(`/api/articles/${articleId}`, { method: "DELETE" });
}

export function extractArticleFromUrl(url: string) {
  return apiRequest<ArticleExtractResult>("/api/articles/extract", {
    method: "POST",
    body: JSON.stringify({ url }),
  });
}

export function createArticle(payload: ArticleEditorValues) {
  return apiRequest<ArticleDetailData>("/api/articles", {
    method: "POST",
    body: JSON.stringify({
      category: payload.category,
      title: normalizeRequired(payload.title),
      content: normalizeRequired(payload.content),
      source: normalizeOptional(payload.source, null),
      originalUrl: normalizeOptional(payload.originalUrl, null),
      thumbnailUrl: normalizeOptional(payload.thumbnailUrl, null),
      videoEmbedUrl: normalizeOptional(payload.videoEmbedUrl, null),
    }),
  });
}

export function updateArticle(articleId: number, payload: ArticleEditorValues) {
  return apiRequest<ArticleDetailData>(`/api/articles/${articleId}`, {
    method: "PATCH",
    body: JSON.stringify({
      category: payload.category,
      title: normalizeRequired(payload.title),
      content: normalizeRequired(payload.content),
      source: normalizeOptional(payload.source, ""),
      originalUrl: normalizeOptional(payload.originalUrl, ""),
      thumbnailUrl: normalizeOptional(payload.thumbnailUrl, ""),
      videoEmbedUrl: normalizeOptional(payload.videoEmbedUrl, ""),
    }),
  });
}
