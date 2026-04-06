import { useEffect, useState } from "react";

import { CATEGORY_LABELS } from "../../shared/constants/categories";
import type { ArticleDetailData } from "../../shared/types/article";
import type { CurrentUser } from "../../shared/types/member";
import { formatDateTime, isRealUrl } from "../../shared/utils/format";

interface ArticleDetailSectionProps {
  article: ArticleDetailData | null;
  currentUser: CurrentUser | null;
  isSaved: boolean;
  onToggleSave: (articleId: number) => void;
  onGenerateSummary: (articleId: number) => void;
  onEditArticle?: () => void;
  onDeleteArticle: (articleId: number) => void;
}

export function ArticleDetailSection({
  article,
  currentUser,
  isSaved,
  onToggleSave,
  onGenerateSummary,
  onEditArticle,
  onDeleteArticle,
}: ArticleDetailSectionProps) {
  const [ttsState, setTtsState] = useState<"idle" | "playing" | "paused">("idle");

  useEffect(() => {
    return () => {
      window.speechSynthesis.cancel();
    };
  }, []);

  if (!article) {
    return <article id="article-detail"><div className="loading-msg">기사를 불러오는 중...</div></article>;
  }

  const currentArticle = article;
  const isOwner = Boolean(currentUser && article.writerId === currentUser.id);
  const summaryMeta = article.summary?.summarySource === "SEED"
    ? { label: "📌 샘플 요약", hint: "시연용 샘플 데이터입니다" }
    : { label: "✦ AI 요약", hint: "AI가 자동으로 요약했습니다" };
  const bodyParagraphs = article.content
    .split("\n")
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);

  function playTts() {
    const text = `${currentArticle.title}.\n${currentArticle.content}`;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "ko-KR";
    utterance.rate = 1;
    utterance.onend = () => setTtsState("idle");
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
    setTtsState("playing");
  }

  function togglePause() {
    if (window.speechSynthesis.paused) {
      window.speechSynthesis.resume();
      setTtsState("playing");
      return;
    }

    window.speechSynthesis.pause();
    setTtsState("paused");
  }

  function stopTts() {
    window.speechSynthesis.cancel();
    setTtsState("idle");
  }

  return (
    <article id="article-detail">
      <div className="article-meta">
        <span className="cat-badge">{CATEGORY_LABELS[article.category]}</span>
        <span className="article-source">{formatDateTime(article.publishedAt)}</span>
        {article.source ? <span className="article-source">· {article.source}</span> : null}
      </div>
      <h1 className="article-title">{article.title}</h1>

      {article.videoEmbedUrl ? (
        <div className="article-video">
          <iframe
            src={article.videoEmbedUrl}
            title={article.title}
            allowFullScreen
          />
        </div>
      ) : (
        <div className={`article-img${article.thumbnailUrl ? " has-img" : ""}`}>
          {article.thumbnailUrl ? <img src={article.thumbnailUrl} alt="" /> : null}
        </div>
      )}

      {article.summary ? (
        <div className="ai-box">
          <div className="ai-box-header">
            <span className="ai-badge-lg">{summaryMeta.label}</span>
            <span className="ai-box-hint">{summaryMeta.hint}</span>
            {article.summary.modelName ? (
              <span style={{ fontSize: "11px", color: "var(--ash)" }}>
                model: {article.summary.modelName}
              </span>
            ) : null}
            {isOwner ? (
              <button
                className="btn btn-ghost"
                style={{ marginLeft: "auto", fontSize: "12px", padding: "4px 10px" }}
                onClick={() => onGenerateSummary(article.id)}
              >
                재생성
              </button>
            ) : null}
          </div>
          <p className="ai-box-text">
            {article.summary.summaryLine1}
            <br />
            {article.summary.summaryLine2}
            <br />
            {article.summary.summaryLine3}
          </p>
          <hr className="ai-box-divider" />
          <p className="ai-key-label">핵심 포인트</p>
          <ul className="ai-key-list">
            <li className="ai-key-item">
              <span className="ai-key-dot" />
              <span className="ai-key-text">{article.summary.keyPoint1}</span>
            </li>
            <li className="ai-key-item">
              <span className="ai-key-dot" />
              <span className="ai-key-text">{article.summary.keyPoint2}</span>
            </li>
            <li className="ai-key-item">
              <span className="ai-key-dot" />
              <span className="ai-key-text">{article.summary.keyPoint3}</span>
            </li>
          </ul>
        </div>
      ) : isOwner ? (
        <div className="ai-box" style={{ textAlign: "center", padding: "24px" }}>
          <p style={{ color: "var(--ash)", marginBottom: "12px" }}>아직 AI 요약이 없습니다</p>
          <button className="btn btn-solid" onClick={() => onGenerateSummary(article.id)}>
            AI 요약 생성
          </button>
        </div>
      ) : null}

      <div className="article-body">
        {bodyParagraphs.length > 0 ? bodyParagraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>) : <p className="muted">본문이 없습니다.</p>}
      </div>
      <div className="article-actions">
        <button
          className={isSaved ? "btn btn-solid btn-md btn-save-active" : "btn btn-solid btn-md"}
          onClick={() => onToggleSave(article.id)}
        >
          {isSaved ? "저장됨" : "저장하기"}
        </button>
        {isRealUrl(article.originalUrl) ? (
          <a className="btn btn-ghost btn-md" href={article.originalUrl ?? "#"} target="_blank" rel="noreferrer">
            원문 기사 보기 →
          </a>
        ) : null}
        {"speechSynthesis" in window ? (
          <>
            {ttsState === "idle" ? (
              <button className="btn btn-ghost btn-md" onClick={playTts}>
                🔊 읽어주기
              </button>
            ) : (
              <button className="btn btn-ghost btn-md" onClick={togglePause}>
                {ttsState === "paused" ? "▶ 재개" : "⏸ 일시정지"}
              </button>
            )}
            {ttsState !== "idle" ? (
              <button className="btn btn-ghost btn-md" onClick={stopTts}>
                ⏹ 중지
              </button>
            ) : null}
          </>
        ) : null}
        {isOwner ? (
          <>
            {onEditArticle ? (
              <button className="btn btn-ghost btn-md" onClick={onEditArticle}>
                수정
              </button>
            ) : null}
            <button
              className="btn btn-ghost btn-md"
              style={{ color: "var(--error)" }}
              onClick={() => onDeleteArticle(article.id)}
            >
              삭제
            </button>
          </>
        ) : null}
      </div>
      {article.writerNickname ? (
        <p style={{ marginTop: "8px", fontSize: "13px", color: "var(--ash)" }}>
          작성자: {article.writerNickname}
        </p>
      ) : null}
    </article>
  );
}
