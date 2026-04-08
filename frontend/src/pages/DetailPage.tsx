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
  articleLoading?: boolean;
  articleErrorMessage?: string;
  relatedLoading?: boolean;
  relatedErrorMessage?: string;
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
  articleLoading = false,
  articleErrorMessage = "",
  relatedLoading = false,
  relatedErrorMessage = "",
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
          {articleLoading ? <article id="article-detail"><div className="loading-msg">기사를 불러오는 중...</div></article> : null}
          {!articleLoading && articleErrorMessage ? <article id="article-detail"><div className="loading-msg">{articleErrorMessage}</div></article> : null}
          {!articleLoading && !articleErrorMessage ? (
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
          <RelatedNewsSection
            articles={relatedArticles}
            loading={relatedLoading}
            errorMessage={relatedErrorMessage}
            onArticleClick={onArticleClick}
          />
        </div>
      </div>
    </div>
  );
}
