package org.example.newssummaryproject.domain.news.dto;

import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.Category;

import java.time.LocalDateTime;

/**
 * 기사 목록 화면에서 카드 하나에 필요한 정보만 담는 응답 DTO다.
 * 본문(content)은 목록에서 필요 없으므로 빼 두었다.
 * summaryPreview는 AI 요약 첫째 줄로, 카드에 미리보기로 표시된다.
 */
public record ArticleListResponse(
        Long id,
        Category category,
        String title,
        String source,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        String summaryPreview
) {
    public static ArticleListResponse from(Article article) {
        return from(article, null);
    }

    public static ArticleListResponse from(Article article, String summaryPreview) {
        return new ArticleListResponse(
                article.getId(),
                article.getCategory(),
                article.getTitle(),
                article.getSource(),
                article.getThumbnailUrl(),
                article.getPublishedAt(),
                summaryPreview
        );
    }
}
