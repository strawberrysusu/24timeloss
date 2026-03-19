package org.example.newssummaryproject.domain.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.newssummaryproject.domain.news.Category;

/**
 * 로그인한 사용자가 기사를 직접 등록할 때 보내는 요청 DTO다.
 *
 * 필수 값: category, title, content
 * 나머지는 선택 — 안 넣으면 기본값이 들어간다.
 *
 * AI 요약(summaryLine1~3, keyPoint1~3)은 선택사항이다.
 * 넣으면 기사에 AI 요약이 함께 표시되고, 안 넣으면 요약 없이 노출된다.
 */
public record CreateArticleRequest(
        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 2, max = 300, message = "제목은 2자 이상 300자 이하여야 합니다.")
        String title,

        @NotBlank(message = "본문은 필수입니다.")
        @Size(min = 10, message = "본문은 10자 이상이어야 합니다.")
        String content,

        // 선택: 언론사 이름 (미입력 시 "직접 등록")
        @Size(max = 100, message = "출처는 100자 이하여야 합니다.")
        String source,

        // 선택: 원문 기사 URL (없으면 null — 원문보기 버튼이 숨겨진다)
        @Size(max = 500, message = "URL은 500자 이하여야 합니다.")
        String originalUrl,

        // 선택: 대표 이미지 URL
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String thumbnailUrl,

        // 선택: AI 요약 3줄
        @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
        String summaryLine1,
        @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
        String summaryLine2,
        @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
        String summaryLine3,

        // 선택: 핵심 포인트 3개
        @Size(max = 500, message = "핵심 포인트는 500자 이하여야 합니다.")
        String keyPoint1,
        @Size(max = 500, message = "핵심 포인트는 500자 이하여야 합니다.")
        String keyPoint2,
        @Size(max = 500, message = "핵심 포인트는 500자 이하여야 합니다.")
        String keyPoint3
) {
}
