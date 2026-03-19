package org.example.newssummaryproject.domain.member;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.example.newssummaryproject.domain.member.dto.MyPageResponse;
import org.example.newssummaryproject.domain.member.dto.ReadHistoryResponse;
import org.example.newssummaryproject.domain.member.dto.UpdateNicknameRequest;
import org.example.newssummaryproject.domain.member.dto.UpdatePasswordRequest;
import org.example.newssummaryproject.domain.news.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * ── 마이페이지 컨트롤러 ──
 *
 * 흐름: 브라우저/프론트 → ★ MyPageController → MyPageService / MemberService → DB
 *
 * 이 컨트롤러의 모든 API는 로그인이 필수다.
 * 모든 메서드에서 MemberController.getLoginMemberId(session)을 호출한다.
 *
 * 두 개의 서비스를 사용하는 이유:
 *   - MyPageService: 저장기사, 관심분야, 읽기기록 등 마이페이지 고유 기능
 *   - MemberService: 닉네임/비밀번호 변경은 회원 도메인의 책임이므로 MemberService에 위임
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final MemberService memberService;

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
     * 저장한 기사 ID 목록 조회 (프론트에서 저장 버튼 상태 표시용)
     * GET /api/mypage/saved-article-ids
     */
    @GetMapping("/saved-article-ids")
    public ResponseEntity<List<Long>> getSavedArticleIds(HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        return ResponseEntity.ok(myPageService.getSavedArticleIds(memberId));
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

    /**
     * 닉네임 변경
     * PATCH /api/mypage/nickname
     */
    @PatchMapping("/nickname")
    public ResponseEntity<MemberResponse> updateNickname(
            @Valid @RequestBody UpdateNicknameRequest request, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        return ResponseEntity.ok(memberService.updateNickname(memberId, request.nickname()));
    }

    /**
     * 비밀번호 변경
     * PATCH /api/mypage/password
     */
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request, HttpSession session) {
        Long memberId = MemberController.getLoginMemberId(session);
        memberService.updatePassword(memberId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
