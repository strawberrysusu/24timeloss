import type { ArticleCardData, ArticleDetailData } from "../shared/types/article";
import type { CurrentUser } from "../shared/types/member";
import { ArticleDetailSection } from "../components/detail/ArticleDetailSection";
import { DetailBreadcrumb } from "../components/detail/DetailBreadcrumb";
import { RelatedNewsSection } from "../components/detail/RelatedNewsSection";

interface DetailPageProps {
  article: ArticleDetailData | null;
  relatedArticles: ArticleCardData[];
  currentUser: CurrentUser | null;
  savedArticleIds: Set<number>;
  loading?: boolean;
  errorMessage?: string;
  onHomeClick: () => void;
  onCategoryClick: () => void;
  onToggleSave: (articleId: number) => void;
  onGenerateSummary: (articleId: number) => void;
  onEditArticle?: () => void;
  onDeleteArticle: (articleId: number) => void;
  onArticleClick: (articleId: number) => void;
}

export function DetailPage({
  article,
  relatedArticles,
  currentUser,
  savedArticleIds,
  loading = false,
  errorMessage = "",
  onHomeClick,
  onCategoryClick,
  onToggleSave,
  onGenerateSummary,
  onEditArticle,
  onDeleteArticle,
  onArticleClick,
}: DetailPageProps) {
  return (
    <div id="page-detail" className="page active">
      <DetailBreadcrumb article={article} onHomeClick={onHomeClick} onCategoryClick={onCategoryClick} />
      <div className="container">
        <div className="detail-grid">
          {loading ? <article id="article-detail"><div className="loading-msg">기사를 불러오는 중...</div></article> : null}
          {!loading && errorMessage ? <article id="article-detail"><div className="loading-msg">{errorMessage}</div></article> : null}
          {!loading && !errorMessage ? (
            <ArticleDetailSection
              article={article}
              currentUser={currentUser}
              isSaved={article ? savedArticleIds.has(article.id) : false}
              onToggleSave={onToggleSave}
              onGenerateSummary={onGenerateSummary}
              onEditArticle={onEditArticle}
              onDeleteArticle={onDeleteArticle}
            />
          ) : null}
          <RelatedNewsSection articles={relatedArticles} onArticleClick={onArticleClick} />
        </div>
      </div>
    </div>
  );
}
