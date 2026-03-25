package org.example.newssummaryproject.domain.news.dto;

import jakarta.validation.constraints.Size;
import org.example.newssummaryproject.domain.news.Category;

/**
 * 기사 수정 요청 DTO다.
 * 모든 필드가 선택사항이며, 값이 있는 필드만 업데이트된다.
 *
 * AI 요약은 "AI 요약 생성" 버튼으로만 관리하므로 요약 필드는 없다.
 */
public record UpdateArticleRequest(
        Category category,

        @Size(min = 2, max = 300, message = "제목은 2자 이상 300자 이하여야 합니다.")
        String title,

        @Size(min = 10, message = "본문은 10자 이상이어야 합니다.")
        String content,

        @Size(max = 100, message = "출처는 100자 이하여야 합니다.")
        String source,

        @Size(max = 500, message = "URL은 500자 이하여야 합니다.")
        String originalUrl
) {
}
