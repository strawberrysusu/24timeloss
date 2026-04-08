import type { Category } from "../constants/categories";
import type { ArticleCardData } from "./article";

export interface CurrentUser {
  id: number;
  email: string;
  nickname: string;
}

export interface LoginResponseData {
  token: string;
  id: number;
  email: string;
  nickname: string;
}

export interface MyPageData {
  nickname: string;
  email: string;
  readArticleCount: number;
  streak: number;
  interests: Category[];
  savedArticles: ArticleCardData[];
}
