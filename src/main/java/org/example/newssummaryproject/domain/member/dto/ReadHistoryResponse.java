package org.example.newssummaryproject.domain.member.dto;

import org.example.newssummaryproject.domain.member.ArticleReadHistory;
import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.Category;

import java.time.LocalDateTime;

public record ReadHistoryResponse(
        Long articleId,
        Category category,
        String title,
        String source,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        LocalDateTime readAt
) {
    public static ReadHistoryResponse from(ArticleReadHistory history) {
        Article a = history.getArticle();
        return new ReadHistoryResponse(
                a.getId(), a.getCategory(), a.getTitle(), a.getSource(),
                a.getThumbnailUrl(), a.getPublishedAt(), history.getCreatedAt()
        );
    }
}
