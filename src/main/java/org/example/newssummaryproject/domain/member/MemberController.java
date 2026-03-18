package org.example.newssummaryproject.domain.member;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.LoginRequest;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.example.newssummaryproject.domain.member.dto.SignupRequest;
import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입
     * POST /api/members/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.signup(request));
    }

    /**
     * 로그인 — 성공 시 세션에 memberId를 저장한다.
     * POST /api/members/login
     */
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        MemberResponse response = memberService.login(request);
        session.setAttribute("memberId", response.id());
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃 — 세션을 제거한다.
     * POST /api/members/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 로그인된 회원 정보 조회
     * GET /api/members/me
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(HttpSession session) {
        Long memberId = getLoginMemberId(session);
        return ResponseEntity.ok(memberService.getMember(memberId));
    }

    public static Long getLoginMemberId(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return memberId;
    }
}
