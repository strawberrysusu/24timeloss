package org.example.newssummaryproject.domain.news;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.MemberController;
import org.example.newssummaryproject.domain.news.dto.ArticleDetailResponse;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;
import org.example.newssummaryproject.domain.news.dto.CreateArticleRequest;
import org.example.newssummaryproject.domain.news.dto.UpdateArticleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * ── 기사 컨트롤러 (기사 관련 HTTP API 진입점) ──
 *
 * 흐름: 브라우저/프론트 → ★ ArticleController → ArticleService → Repository → DB
 *
 * 이 컨트롤러의 역할:
 *   1. HTTP 요청 파라미터를 파싱한다 (@PathVariable, @RequestParam, @RequestBody)
 *   2. 로그인이 필요한 API는 Spring Security의 SecurityContext에서 memberId를 꺼낸다
 *   3. ArticleService에 비즈니스 로직을 위임한다
 *   4. 결과를 JSON으로 응답한다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleExtractService articleExtractService;

    @GetMapping
    public ResponseEntity<Page<ArticleListResponse>> getArticles(
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.getArticles(category, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticle(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleListResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.search(keyword, pageable));
    }

    /**
     * 추천 기사 — GET /api/articles/recommendations?page=0&size=10
     *
     * 로그인 상태면 관심 카테고리 기반으로 추천하고,
     * 비로그인이면 전체 최신 기사를 반환한다.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Page<ArticleListResponse>> recommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long memberId = MemberController.getLoginMemberIdOrNull();
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.recommend(memberId, pageable));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<ArticleListResponse>> getRelated(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getRelatedArticles(id));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<ArticleListResponse>> getTrending() {
        return ResponseEntity.ok(articleService.getTrending());
    }

    @GetMapping("/briefing")
    public ResponseEntity<Map<String, Object>> getBriefing() {
        return ResponseEntity.ok(articleService.getBriefing());
    }

    @GetMapping("/popular-keywords")
    public ResponseEntity<List<String>> getPopularKeywords() {
        return ResponseEntity.ok(articleService.getPopularKeywords());
    }

    // ── URL에서 기사 자동 추출 (로그인 필수) ──

    @PostMapping("/extract")
    public ResponseEntity<ArticleExtractService.ExtractResult> extractFromUrl(
            @RequestBody Map<String, String> request) {
        MemberController.getLoginMemberId();  // 로그인 확인
        String url = request.get("url");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL을 입력해주세요.");
        }
        return ResponseEntity.ok(articleExtractService.extract(url.trim()));
    }

    // ── AI 요약 생성 (로그인 필수) ──

    @PostMapping("/{id}/generate-summary")
    public ResponseEntity<ArticleDetailResponse> generateSummary(@PathVariable Long id) {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.ok(articleService.generateSummary(memberId, id));
    }

    // ── 기사 등록 / 수정 / 삭제 (로그인 필수) ──

    @PostMapping
    public ResponseEntity<ArticleDetailResponse> createArticle(
            @Valid @RequestBody CreateArticleRequest request) {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.createArticle(memberId, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateArticleRequest request) {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.ok(articleService.updateArticle(memberId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        Long memberId = MemberController.getLoginMemberId();
        articleService.deleteArticle(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
