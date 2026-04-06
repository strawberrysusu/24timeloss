import { CATEGORY_LABELS } from "../../shared/constants/categories";
import type { ArticleCardData } from "../../shared/types/article";
import { formatDate } from "../../shared/utils/format";

interface NewsFeedSectionProps {
  articles: ArticleCardData[];
  isLastPage: boolean;
  savedArticleIds: Set<number>;
  onArticleClick: (articleId: number) => void;
  onToggleSave: (articleId: number) => void;
  onLoadMore: () => void;
}

export function NewsFeedSection({
  articles,
  isLastPage,
  savedArticleIds,
  onArticleClick,
  onToggleSave,
  onLoadMore,
}: NewsFeedSectionProps) {
  return (
    <section className="news-feed" id="news-feed">
      {articles.length === 0 ? <p className="loading-msg">표시할 기사가 없습니다.</p> : null}
      {articles.map((article) => {
        const isSaved = savedArticleIds.has(article.id);
        return (
          <article
            key={article.id}
            className="news-card"
            onClick={() => onArticleClick(article.id)}
          >
            <div className="news-card-body">
              <div className="news-card-meta">
                <span className="cat-badge">{CATEGORY_LABELS[article.category]}</span>
                <span className="news-card-source">
                  {formatDate(article.publishedAt)}
                  {article.source ? ` · ${article.source}` : ""}
                </span>
              </div>
              <h2 className="news-card-title">{article.title}</h2>
              {article.summaryPreview ? (
                <p className="news-card-summary">✦ {article.summaryPreview}</p>
              ) : null}
              <div className="news-card-actions">
                <button
                  className="btn btn-solid btn-action"
                  onClick={(event) => {
                    event.stopPropagation();
                    onArticleClick(article.id);
                  }}
                >
                  전체 읽기
                </button>
                <button
                  className={isSaved ? "btn btn-solid btn-action btn-save-active" : "btn btn-ghost btn-action"}
                  onClick={(event) => {
                    event.stopPropagation();
                    onToggleSave(article.id);
                  }}
                >
                  {isSaved ? "🔖 저장됨" : "☆ 저장"}
                </button>
              </div>
            </div>
            <div className="news-card-img">
              {article.thumbnailUrl ? (
                <img
                  src={article.thumbnailUrl}
                  alt=""
                  style={{ width: "100%", height: "100%", objectFit: "cover" }}
                />
              ) : (
                <svg width="36" height="36" fill="none" stroke="currentColor" strokeWidth="1.2" viewBox="0 0 24 24">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m21 15-5-5L5 21" />
                </svg>
              )}
            </div>
          </article>
        );
      })}
      {isLastPage ? null : (
        <button className="btn btn-ghost btn-md load-more-btn" onClick={onLoadMore}>
          더보기
        </button>
      )}
    </section>
  );
}
