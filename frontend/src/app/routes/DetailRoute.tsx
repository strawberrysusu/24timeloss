import { useState } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";

import { ArticleEditorModal } from "../../components/modal/ArticleEditorModal";
import { useDetailPageData } from "../../features/detail/useDetailPageData";
import { DetailPage } from "../../pages/DetailPage";
import { extractArticleFromUrl, updateArticle } from "../../shared/api/articles";
import { CATEGORY_OPTIONS } from "../../shared/constants/categories";
import type { ArticleDetailData } from "../../shared/types/article";
import type { CurrentUser } from "../../shared/types/member";

function toArticleEditorValues(article: ArticleDetailData) {
  return {
    category: article.category,
    title: article.title,
    source: article.source ?? "",
    originalUrl: article.originalUrl ?? "",
    thumbnailUrl: article.thumbnailUrl ?? "",
    videoEmbedUrl: article.videoEmbedUrl ?? "",
    content: article.content,
  };
}

interface DetailRouteProps {
  currentUser: CurrentUser | null;
  savedArticleIds: Set<number>;
  onToggleSave: (articleId: number) => Promise<void>;
  onAuthenticationRequired: () => void;
}

export function DetailRoute({
  currentUser,
  savedArticleIds,
  onToggleSave,
  onAuthenticationRequired,
}: DetailRouteProps) {
  const navigate = useNavigate();
  const params = useParams<{ id: string }>();
  const articleId = Number(params.id);
  const [isEditorOpen, setIsEditorOpen] = useState(false);

  const detail = useDetailPageData({
    articleId,
    currentUserId: currentUser?.id ?? null,
    onDeleted: () => navigate("/"),
    onAuthenticationRequired,
  });

  if (!Number.isFinite(articleId)) {
    return <Navigate to="/" replace />;
  }

  const editableArticle = detail.article;

  return (
    <>
      <DetailPage
        article={detail.article}
        relatedArticles={detail.relatedArticles}
        currentUser={currentUser}
        savedArticleIds={savedArticleIds}
        loading={detail.loading}
        errorMessage={detail.errorMessage}
        onHomeClick={() => navigate("/")}
        onCategoryClick={() => navigate("/")}
        onToggleSave={onToggleSave}
        onGenerateSummary={() => void detail.regenerateSummary()}
        onEditArticle={
          editableArticle && currentUser && editableArticle.writerId === currentUser.id
            ? () => setIsEditorOpen(true)
            : undefined
        }
        onDeleteArticle={() => {
          if (!window.confirm("정말 이 기사를 삭제하시겠습니까?")) {
            return;
          }
          void detail.removeArticle();
        }}
        onArticleClick={(nextArticleId) => navigate(`/detail/${nextArticleId}`)}
      />
      {editableArticle ? (
        <ArticleEditorModal
          open={isEditorOpen}
          mode="edit"
          categories={CATEGORY_OPTIONS}
          initialValues={toArticleEditorValues(editableArticle)}
          onClose={() => setIsEditorOpen(false)}
          onExtract={extractArticleFromUrl}
          onSubmit={async (values) => {
            await updateArticle(editableArticle.id, values);
            setIsEditorOpen(false);
            detail.refresh();
          }}
        />
      ) : null}
    </>
  );
}
