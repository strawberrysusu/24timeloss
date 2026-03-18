package org.example.newssummaryproject.domain.news;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.news.dto.ArticleDetailResponse;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 기사 목록 조회
     * GET /api/articles              → 전체 최신순
     * GET /api/articles?category=IT_SCIENCE → 카테고리별 최신순
     */
    @GetMapping
    public ResponseEntity<List<ArticleListResponse>> getArticles(
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(articleService.getArticles(category));
    }

    /**
     * 기사 상세 조회
     * GET /api/articles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticle(id));
    }

    /**
     * 키워드 검색
     * GET /api/articles/search?keyword=AI
     */
    @GetMapping("/search")
    public ResponseEntity<List<ArticleListResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(articleService.search(keyword));
    }

    /**
     * 추천 기사 (관심 카테고리 기반, 비로그인 시 전체 최신)
     * GET /api/articles/recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<ArticleListResponse>> recommendations(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        return ResponseEntity.ok(articleService.recommend(memberId));
    }
}
