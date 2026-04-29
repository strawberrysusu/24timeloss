package org.example.newssummaryproject.domain.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.newssummaryproject.domain.news.Category;

/**
 * 로그인한 사용자가 기사를 직접 등록할 때 보내는 요청 DTO다.
 *
 * 필수 값: category, title, content
 * 나머지는 선택 — 안 넣으면 기본값이 들어간다.
 *
 * AI 요약은 기사 등록 후 "AI 요약 생성" 버튼으로만 만들 수 있다.
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
        @Pattern(regexp = "^$|^https?://.+", message = "URL은 http:// 또는 https://로 시작해야 합니다.")
        String originalUrl,

        // 선택: 대표 이미지 URL
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|^https?://.+", message = "이미지 URL은 http:// 또는 https://로 시작해야 합니다.")
        String thumbnailUrl,

        // 선택: 영상 임베드 URL — iframe으로 렌더되므로 임베드 전용 도메인만 허용한다.
        // 외부 URL을 자유롭게 허용하면 피싱/추적/clickjacking 통로가 된다.
        @Size(max = 500, message = "영상 URL은 500자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^https://(tv\\.naver\\.com/embed/|www\\.youtube\\.com/embed/|www\\.youtube-nocookie\\.com/embed/).+",
                message = "영상 URL은 네이버 TV 또는 YouTube 임베드 주소만 허용됩니다."
        )
        String videoEmbedUrl
) {
}
