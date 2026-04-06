import type { Category } from "../constants/categories";

export interface ArticleCardData {
  id: number;
  category: Category;
  title: string;
  source: string | null;
  thumbnailUrl: string | null;
  publishedAt: string;
  summaryPreview: string | null;
}

export interface ArticleSummaryData {
  summaryLine1: string;
  summaryLine2: string;
  summaryLine3: string;
  keyPoint1: string;
  keyPoint2: string;
  keyPoint3: string;
  summarySource: "AI_GENERATED" | "SEED";
  modelName: string | null;
  generatedAt: string | null;
}

export interface ArticleDetailData {
  id: number;
  category: Category;
  title: string;
  source: string | null;
  originalUrl: string | null;
  thumbnailUrl: string | null;
  hasVideo: boolean;
  videoEmbedUrl: string | null;
  content: string;
  publishedAt: string;
  writerId: number | null;
  writerNickname: string | null;
  summary: ArticleSummaryData | null;
}

export interface ReadHistoryItem {
  articleId: number;
  category: Category;
  title: string;
  source: string | null;
  thumbnailUrl: string | null;
  publishedAt: string;
  readAt: string;
}

export interface ArticleEditorValues {
  category: Category | "";
  title: string;
  source: string;
  originalUrl: string;
  thumbnailUrl: string;
  videoEmbedUrl: string;
  content: string;
}

export interface ArticleExtractResult {
  title?: string | null;
  source?: string | null;
  thumbnailUrl?: string | null;
  content?: string | null;
  videoEmbedUrl?: string | null;
}
