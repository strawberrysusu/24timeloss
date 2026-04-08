import { CATEGORY_LABELS } from "../../shared/constants/categories";
import type { ArticleDetailData } from "../../shared/types/article";

interface DetailBreadcrumbProps {
  article: ArticleDetailData | null;
  onHomeClick: () => void;
  onCategoryClick: () => void;
}

export function DetailBreadcrumb({
  article,
  onHomeClick,
  onCategoryClick,
}: DetailBreadcrumbProps) {
  return (
    <div className="breadcrumb">
      <div className="container">
        <div className="breadcrumb-inner" id="detail-breadcrumb">
          <button onClick={onHomeClick}>홈</button>
          <span className="sep">/</span>
          <button onClick={onCategoryClick}>
            {article ? CATEGORY_LABELS[article.category] : "기사"}
          </button>
          <span className="sep">/</span>
          <span className="current">
            {article ? (article.title.length > 30 ? `${article.title.slice(0, 30)}...` : article.title) : "기사"}
          </span>
        </div>
      </div>
    </div>
  );
}
