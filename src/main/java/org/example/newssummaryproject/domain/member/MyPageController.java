package org.example.newssummaryproject.domain.member;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.MyPageResponse;
import org.example.newssummaryproject.domain.member.dto.ReadHistoryResponse;
import org.example.newssummaryproject.domain.news.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 마이페이지 종합 조회
     * GET /api/mypage
     */
    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage(HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        return ResponseEntity.ok(myPageService.getMyPage(memberId));
    }

    /**
     * 기사 저장
     * POST /api/mypage/saved-articles/{articleId}
     */
    @PostMapping("/saved-articles/{articleId}")
    public ResponseEntity<Void> saveArticle(@PathVariable Long articleId, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        myPageService.saveArticle(memberId, articleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 기사 저장 취소
     * DELETE /api/mypage/saved-articles/{articleId}
     */
    @DeleteMapping("/saved-articles/{articleId}")
    public ResponseEntity<Void> unsaveArticle(@PathVariable Long articleId, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        myPageService.unsaveArticle(memberId, articleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 관심 카테고리 추가
     * POST /api/mypage/interests/{category}
     */
    @PostMapping("/interests/{category}")
    public ResponseEntity<Void> addInterest(@PathVariable Category category, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        myPageService.addInterest(memberId, category);
        return ResponseEntity.ok().build();
    }

    /**
     * 관심 카테고리 삭제
     * DELETE /api/mypage/interests/{category}
     */
    @DeleteMapping("/interests/{category}")
    public ResponseEntity<Void> removeInterest(@PathVariable Category category, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        myPageService.removeInterest(memberId, category);
        return ResponseEntity.ok().build();
    }

    /**
     * 읽기 기록 저장
     * POST /api/mypage/read-history/{articleId}
     */
    @PostMapping("/read-history/{articleId}")
    public ResponseEntity<Void> recordRead(@PathVariable Long articleId, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        myPageService.recordRead(memberId, articleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 읽은 기사 목록 조회
     * GET /api/mypage/read-history
     */
    @GetMapping("/read-history")
    public ResponseEntity<List<ReadHistoryResponse>> getReadHistory(HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        return ResponseEntity.ok(myPageService.getReadHistory(memberId));
    }
}
