package org.example.newssummaryproject.domain.news.dto;

import jakarta.validation.constraints.Pattern;
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
        @Pattern(regexp = "^$|^https?://.+", message = "URL은 http:// 또는 https://로 시작해야 합니다.")
        String originalUrl,

        // 선택: 대표 이미지 URL
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|^https?://.+", message = "이미지 URL은 http:// 또는 https://로 시작해야 합니다.")
        String thumbnailUrl,

        // 선택: 영상 임베드 URL — iframe으로 렌더되므로 임베드 전용 도메인만 허용한다.
        @Size(max = 500, message = "영상 URL은 500자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^https://(tv\\.naver\\.com/embed/|www\\.youtube\\.com/embed/|www\\.youtube-nocookie\\.com/embed/).+",
                message = "영상 URL은 네이버 TV 또는 YouTube 임베드 주소만 허용됩니다."
        )
        String videoEmbedUrl
) {
}
