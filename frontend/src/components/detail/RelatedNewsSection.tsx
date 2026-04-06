import { formatDate } from "../../shared/utils/format";
import { CATEGORY_LABELS } from "../../shared/constants/categories";
import type { ArticleCardData } from "../../shared/types/article";

interface RelatedNewsSectionProps {
  articles: ArticleCardData[];
  onArticleClick: (articleId: number) => void;
}

export function RelatedNewsSection({
  articles,
  onArticleClick,
}: RelatedNewsSectionProps) {
  return (
    <aside className="related-news" id="related-news">
      <h3 className="related-title">관련 뉴스</h3>
      {articles.length === 0 ? <p className="loading-msg">관련 뉴스가 없습니다.</p> : null}
      {articles.map((article) => (
        <button
          key={article.id}
          type="button"
          className="related-item"
          onClick={() => onArticleClick(article.id)}
        >
          <div className="related-img">
            {article.thumbnailUrl ? (
              <img
                src={article.thumbnailUrl}
                alt=""
                style={{ width: "100%", height: "100%", objectFit: "cover", borderRadius: "var(--radius-md)" }}
              />
            ) : (
              <svg width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.2" viewBox="0 0 24 24">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <path d="m21 15-5-5L5 21" />
              </svg>
            )}
          </div>
          <span className="cat-badge">{CATEGORY_LABELS[article.category]}</span>
          <p className="related-item-title">{article.title}</p>
          {article.summaryPreview ? (
            <p className="related-item-summary">✦ {article.summaryPreview}</p>
          ) : null}
          <span className="related-item-date">
            {formatDate(article.publishedAt)}
            {article.source ? ` · ${article.source}` : ""}
          </span>
        </button>
      ))}
    </aside>
  );
}
