package org.example.newssummaryproject.domain.news.dto;

import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.ArticleSummary;
import org.example.newssummaryproject.domain.news.Category;

import java.time.LocalDateTime;

/**
 * 기사 상세 페이지에 필요한 전체 정보를 담는 응답 DTO다.
 * AI 요약이 아직 없으면 summary 필드가 null로 내려간다.
 */
public record ArticleDetailResponse(
        Long id,
        Category category,
        String title,
        String source,
        String originalUrl,
        String thumbnailUrl,
        String content,
        LocalDateTime publishedAt,
        Long writerId,
        String writerNickname,
        SummaryResponse summary
) {
    public static ArticleDetailResponse from(Article article, ArticleSummary summary) {
        return new ArticleDetailResponse(
                article.getId(),
                article.getCategory(),
                article.getTitle(),
                article.getSource(),
                article.getOriginalUrl(),
                article.getThumbnailUrl(),
                article.getContent(),
                article.getPublishedAt(),
                article.getWriter() != null ? article.getWriter().getId() : null,
                article.getWriter() != null ? article.getWriter().getNickname() : null,
                summary != null ? SummaryResponse.from(summary) : null
        );
    }

    public record SummaryResponse(
            String summaryLine1,
            String summaryLine2,
            String summaryLine3,
            String keyPoint1,
            String keyPoint2,
            String keyPoint3
    ) {
        public static SummaryResponse from(ArticleSummary s) {
            return new SummaryResponse(
                    s.getSummaryLine1(), s.getSummaryLine2(), s.getSummaryLine3(),
                    s.getKeyPoint1(), s.getKeyPoint2(), s.getKeyPoint3()
            );
        }
    }
}
