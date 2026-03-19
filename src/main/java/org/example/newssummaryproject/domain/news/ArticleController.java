package org.example.newssummaryproject.domain.news;

import jakarta.servlet.http.HttpSession;
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
 *   2. 로그인이 필요한 API는 세션에서 memberId를 꺼낸다
 *   3. ArticleService에 비즈니스 로직을 위임한다
 *   4. 결과를 JSON으로 응답한다
 *
 * 컨트롤러에는 비즈니스 로직을 넣지 않는 것이 원칙이다.
 * "이 요청을 누가 처리할지" 연결만 하는 얇은(thin) 계층이다.
 *
 * 어노테이션은 MemberController.java 참고 (@RestController, @RequestMapping 등).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 기사 목록 조회 (페이지네이션) — GET /api/articles?category=IT_SCIENCE&page=0&size=10
     *
     * @RequestParam: URL의 쿼리 파라미터(?key=value)를 메서드 파라미터로 받는다.
     *   - required = false: category를 안 보내면 null → 전체 기사 조회
     *   - defaultValue = "0": page를 안 보내면 0번 페이지(첫 페이지)
     * Pageable: Spring Data JPA의 페이지네이션 정보를 담는 객체 (몇 번째 페이지, 몇 개씩)
     */
    @GetMapping
    public ResponseEntity<Page<ArticleListResponse>> getArticles(
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.getArticles(category, pageable));
    }

    /**
     * 기사 상세 조회 — GET /api/articles/{id}
     *
     * @PathVariable: URL 경로의 {id} 부분을 Long id로 받는다.
     * 예: GET /api/articles/5 → id = 5
     *
     * 이 API는 로그인 없이도 접근 가능하다 (HttpSession 파라미터 없음).
     * 내부적으로 조회수(viewCount)가 1 올라간다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticle(id));
    }

    /**
     * 키워드 검색 (페이지네이션)
     * GET /api/articles/search?keyword=AI&page=0&size=10
     */
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
     *
     * 여기서는 getLoginMemberId()를 쓰지 않고 직접 세션을 꺼낸다.
     * 이유: 비로그인 사용자도 접근 가능해야 하므로, null이어도 예외를 던지면 안 되기 때문이다.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Page<ArticleListResponse>> recommendations(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long memberId = (Long) session.getAttribute("memberId");
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.recommend(memberId, pageable));
    }

    /**
     * 관련 기사 (같은 카테고리 + 키워드 기반, 최대 5개)
     * GET /api/articles/{id}/related
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<ArticleListResponse>> getRelated(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getRelatedArticles(id));
    }

    /**
     * 트렌딩 기사 (최신 인기 기사 5개)
     * GET /api/articles/trending
     */
    @GetMapping("/trending")
    public ResponseEntity<List<ArticleListResponse>> getTrending() {
        return ResponseEntity.ok(articleService.getTrending());
    }

    /**
     * 오늘의 AI 브리핑
     * GET /api/articles/briefing
     */
    @GetMapping("/briefing")
    public ResponseEntity<Map<String, Object>> getBriefing() {
        return ResponseEntity.ok(articleService.getBriefing());
    }

    /**
     * 인기 검색어 (최근 7일간 가장 많이 검색된 키워드 10개)
     * GET /api/articles/popular-keywords
     */
    @GetMapping("/popular-keywords")
    public ResponseEntity<List<String>> getPopularKeywords() {
        return ResponseEntity.ok(articleService.getPopularKeywords());
    }

    // ── 기사 등록 / 수정 / 삭제 (로그인 필수) ──
    // 아래 3개 API는 모두 MemberController.getLoginMemberId()로 로그인을 강제한다.
    // 비로그인 상태면 401(UNAUTHORIZED) 에러가 자동으로 발생한다.

    /**
     * 새 기사를 등록한다 — POST /api/articles
     *
     * @Valid + @RequestBody: JSON → CreateArticleRequest 변환 + 검증
     * MemberController.getLoginMemberId(session): 세션에서 로그인 회원 ID를 꺼낸다 (없으면 401)
     * 201 Created: "새 리소스가 생성됨"을 나타내는 HTTP 상태코드
     */
    @PostMapping
    public ResponseEntity<ArticleDetailResponse> createArticle(
            @Valid @RequestBody CreateArticleRequest request,
            HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.createArticle(memberId, request));
    }

    /**
     * 기사를 수정한다 — PATCH /api/articles/{id}
     *
     * PATCH를 쓰는 이유: PUT은 "전체 교체", PATCH는 "일부 수정"이다.
     * 기사 수정은 보낸 필드만 수정하고 안 보낸 필드는 유지하므로 PATCH가 적절하다.
     * 작성자 본인이 아니면 ArticleService에서 403(FORBIDDEN) 에러가 발생한다.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateArticleRequest request,
            HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        return ResponseEntity.ok(articleService.updateArticle(memberId, id, request));
    }

    /**
     * 기사를 삭제한다 — DELETE /api/articles/{id}
     *
     * 204 No Content: "삭제 성공했고, 돌려줄 본문은 없다"는 의미.
     * 작성자 본인이 아니면 ArticleService에서 403(FORBIDDEN) 에러가 발생한다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        articleService.deleteArticle(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
