package org.example.newssummaryproject.domain.member.dto;

import org.example.newssummaryproject.domain.news.Category;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;

import java.util.List;

/**
 * 마이페이지에 필요한 종합 정보를 담는 응답 DTO다.
 */
public record MyPageResponse(
        String nickname,
        String email,
        long readArticleCount,
        List<Category> interests,
        List<ArticleListResponse> savedArticles
) {
}
