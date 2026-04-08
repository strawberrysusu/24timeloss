package org.example.newssummaryproject.domain.member;

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
 * 이 컨트롤러의 모든 API는 로그인이 필수다.
 * Spring Security가 SecurityFilterChain에서 인증을 강제하고,
 * MemberController.getLoginMemberId()로 현재 사용자 ID를 꺼낸다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage() {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.ok(myPageService.getMyPage(memberId));
    }

    @GetMapping("/saved-article-ids")
    public ResponseEntity<List<Long>> getSavedArticleIds() {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.ok(myPageService.getSavedArticleIds(memberId));
    }

    @PostMapping("/saved-articles/{articleId}")
    public ResponseEntity<Void> saveArticle(@PathVariable Long articleId) {
        Long memberId = MemberController.getLoginMemberId();
        myPageService.saveArticle(memberId, articleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/saved-articles/{articleId}")
    public ResponseEntity<Void> unsaveArticle(@PathVariable Long articleId) {
        Long memberId = MemberController.getLoginMemberId();
        myPageService.unsaveArticle(memberId, articleId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/interests/{category}")
    public ResponseEntity<Void> addInterest(@PathVariable Category category) {
        Long memberId = MemberController.getLoginMemberId();
        myPageService.addInterest(memberId, category);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/interests/{category}")
    public ResponseEntity<Void> removeInterest(@PathVariable Category category) {
        Long memberId = MemberController.getLoginMemberId();
        myPageService.removeInterest(memberId, category);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-history/{articleId}")
    public ResponseEntity<Void> recordRead(@PathVariable Long articleId) {
        Long memberId = MemberController.getLoginMemberId();
        myPageService.recordRead(memberId, articleId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/read-history")
    public ResponseEntity<List<ReadHistoryResponse>> getReadHistory() {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.ok(myPageService.getReadHistory(memberId));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<MemberResponse> updateNickname(
            @Valid @RequestBody UpdateNicknameRequest request) {
        Long memberId = MemberController.getLoginMemberId();
        return ResponseEntity.ok(memberService.updateNickname(memberId, request.nickname()));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request) {
        Long memberId = MemberController.getLoginMemberId();
        memberService.updatePassword(memberId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
