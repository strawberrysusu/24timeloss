import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { createArticle } from "../../shared/api/articles";
import type { ArticleEditorValues } from "../../shared/types/article";

export function useCreateArticleState() {
  const navigate = useNavigate();
  const [isCreateEditorOpen, setIsCreateEditorOpen] = useState(false);

  function openCreateEditor() {
    setIsCreateEditorOpen(true);
  }

  function closeCreateEditor() {
    setIsCreateEditorOpen(false);
  }

  async function handleCreateArticleSubmit(values: ArticleEditorValues) {
    const created = await createArticle(values);
    setIsCreateEditorOpen(false);
    navigate(`/detail/${created.id}`);
  }

  return {
    isCreateEditorOpen,
    openCreateEditor,
    closeCreateEditor,
    handleCreateArticleSubmit,
  };
}
