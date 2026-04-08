import { useEffect, useState, type FormEvent } from "react";

import type { Category } from "../../shared/constants/categories";
import type { ArticleEditorValues, ArticleExtractResult } from "../../shared/types/article";

interface ArticleEditorModalProps {
  open: boolean;
  mode: "create" | "edit";
  categories: readonly { label: string; value: Category | null }[];
  initialValues: ArticleEditorValues;
  onClose: () => void;
  onExtract: (url: string) => Promise<ArticleExtractResult>;
  onSubmit: (values: ArticleEditorValues) => Promise<void>;
}

export function ArticleEditorModal({
  open,
  mode,
  categories,
  initialValues,
  onClose,
  onExtract,
  onSubmit,
}: ArticleEditorModalProps) {
  const [form, setForm] = useState<ArticleEditorValues>(initialValues);
  const [submitError, setSubmitError] = useState("");
  const [extractStatus, setExtractStatus] = useState("");
  const [extractTone, setExtractTone] = useState<"idle" | "success" | "error">("idle");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isExtracting, setIsExtracting] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }

    setForm(initialValues);
    setSubmitError("");
    setExtractStatus("");
    setExtractTone("idle");
    setIsSubmitting(false);
    setIsExtracting(false);
  }, [initialValues, open, mode]);

  if (!open) {
    return null;
  }

  async function handleExtract() {
    if (!form.originalUrl.trim()) {
      setExtractTone("error");
      setExtractStatus("URL을 입력해주세요.");
      return;
    }

    try {
      setIsExtracting(true);
      setExtractTone("idle");
      setExtractStatus("기사를 가져오고 있습니다...");

      const result = await onExtract(form.originalUrl.trim());
      setForm((previous) => ({
        ...previous,
        title: result.title ?? previous.title,
        source: result.source ?? previous.source,
        thumbnailUrl: result.thumbnailUrl ?? previous.thumbnailUrl,
        content: result.content ?? previous.content,
        videoEmbedUrl: result.videoEmbedUrl ?? previous.videoEmbedUrl,
      }));

      setExtractTone("success");
      setExtractStatus(
        `기사 정보를 불러왔습니다${result.videoEmbedUrl ? " (영상 기사)" : ""}. 확인 후 ${
          mode === "create" ? "등록" : "수정"
        }해주세요.`,
      );
    } catch (error) {
      setExtractTone("error");
      setExtractStatus(error instanceof Error ? error.message : "기사를 가져올 수 없습니다.");
    } finally {
      setIsExtracting(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError("");

    if (!form.category || !form.title.trim() || !form.content.trim()) {
      setSubmitError("카테고리, 제목, 본문은 필수입니다.");
      return;
    }

    try {
      setIsSubmitting(true);
      await onSubmit(form);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "기사 저장에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="modal-overlay show" onClick={onClose}>
      <div className="modal-box" style={{ maxWidth: "520px" }} onClick={(event) => event.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>
          &times;
        </button>
        <h2 className="modal-title">{mode === "create" ? "기사 등록" : "기사 수정"}</h2>
        <p className="modal-desc">뉴스 URL을 넣으면 자동으로 정보를 가져옵니다</p>
        {submitError ? <div className="modal-error">{submitError}</div> : null}
        <form onSubmit={handleSubmit}>
          <div style={{ display: "flex", gap: "8px", marginBottom: "8px" }}>
            <input
              className="modal-input"
              name="url"
              type="text"
              placeholder="뉴스 URL 붙여넣기"
              style={{ flex: 1, marginBottom: 0 }}
              value={form.originalUrl}
              onChange={(event) =>
                setForm((previous) => ({ ...previous, originalUrl: event.target.value }))
              }
            />
            <button
              className="btn btn-solid"
              type="button"
              style={{ whiteSpace: "nowrap", padding: "0 16px" }}
              onClick={() => void handleExtract()}
              disabled={isExtracting}
            >
              {isExtracting ? "불러오는 중..." : "불러오기"}
            </button>
          </div>
          {extractStatus ? (
            <p
              style={{
                fontSize: "12px",
                color:
                  extractTone === "success"
                    ? "var(--success)"
                    : extractTone === "error"
                      ? "var(--error)"
                      : "var(--ash)",
                margin: "0 0 12px 0",
              }}
            >
              {extractStatus}
            </p>
          ) : null}
          <select
            className="modal-input"
            name="category"
            value={form.category}
            onChange={(event) =>
              setForm((previous) => ({
                ...previous,
                category: event.target.value as Category | "",
              }))
            }
          >
            <option value="" disabled>
              카테고리 선택
            </option>
            {categories
              .filter((category) => category.value !== null)
              .map((category) => (
                <option key={category.label} value={category.value ?? ""}>
                  {category.label}
                </option>
              ))}
          </select>
          <input
            className="modal-input"
            name="title"
            type="text"
            placeholder="기사 제목"
            value={form.title}
            onChange={(event) => setForm((previous) => ({ ...previous, title: event.target.value }))}
          />
          <input
            className="modal-input"
            name="source"
            type="text"
            placeholder="출처 (예: 한국일보)"
            value={form.source}
            onChange={(event) => setForm((previous) => ({ ...previous, source: event.target.value }))}
          />
          <input
            className="modal-input"
            name="thumbnailUrl"
            type="text"
            placeholder="대표 이미지 URL"
            value={form.thumbnailUrl}
            onChange={(event) =>
              setForm((previous) => ({ ...previous, thumbnailUrl: event.target.value }))
            }
          />
          <textarea
            className="modal-input"
            name="content"
            rows={6}
            placeholder="기사 본문"
            style={{ resize: "vertical", fontFamily: "inherit" }}
            value={form.content}
            onChange={(event) => setForm((previous) => ({ ...previous, content: event.target.value }))}
          />
          <button className="btn btn-solid modal-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? "저장 중..." : mode === "create" ? "등록하기" : "수정하기"}
          </button>
        </form>
      </div>
    </div>
  );
}
