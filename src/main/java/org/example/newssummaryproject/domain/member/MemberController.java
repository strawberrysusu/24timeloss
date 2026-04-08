package org.example.newssummaryproject.domain.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.LoginRequest;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.example.newssummaryproject.domain.member.dto.SignupRequest;
import org.example.newssummaryproject.global.config.CustomUserDetails;
import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * ── 회원 컨트롤러 (HTTP 요청 진입점) ──
 *
 * 흐름: 브라우저/프론트 → ★ Controller → Service → Repository → DB
 *
 * 인증 방식 — Spring Security 세션 기반:
 *   1. 로그인 성공 → AuthenticationManager가 인증 → SecurityContext에 저장 → 세션에 연결
 *   2. 이후 요청마다 Spring Security가 세션에서 SecurityContext를 자동 복원
 *   3. SecurityContextHolder.getContext().getAuthentication()으로 현재 사용자 확인
 *   4. 로그아웃 → 세션 무효화 + SecurityContext 클리어
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;

    /**
     * 회원가입 — POST /api/members/signup
     *
     * 회원가입 후 자동 로그인: AuthenticationManager로 인증해서 SecurityContext에 저장한다.
     */
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        MemberResponse response = memberService.signup(request);

        // 회원가입 직후 자동 로그인 — Spring Security 인증 처리
        authenticateAndSaveToSession(request.email(), request.password(), httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인 — POST /api/members/login
     *
     * AuthenticationManager가 비밀번호를 검증한다.
     * 성공하면 SecurityContext에 인증 정보를 저장하고 세션에 연결한다.
     * 실패하면 BadCredentialsException → 400 BAD_REQUEST 응답.
     */
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            authenticateAndSaveToSession(request.email(), request.password(), httpRequest);
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        MemberResponse response = memberService.getMemberByEmail(request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃 — POST /api/members/logout
     *
     * 세션을 무효화하고 SecurityContext를 클리어한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 내 정보 조회 — GET /api/members/me
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me() {
        Long memberId = getLoginMemberId();
        return ResponseEntity.ok(memberService.getMember(memberId));
    }

    /**
     * SecurityContext에서 로그인된 회원 ID를 꺼내는 공용 헬퍼 메서드다.
     *
     * static인 이유: ArticleController, MyPageController 등 다른 컨트롤러에서도
     * MemberController.getLoginMemberId() 형태로 호출하기 위해서다.
     */
    public static Long getLoginMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getMemberId();
    }

    /**
     * SecurityContext에서 로그인된 회원 ID를 꺼내되, 비로그인이면 null을 반환한다.
     * 추천 기사 등 비로그인도 허용하는 API에서 사용한다.
     */
    public static Long getLoginMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getMemberId();
    }

    /**
     * 이메일/비밀번호로 인증하고, SecurityContext에 저장 후 세션에 연결하는 내부 헬퍼다.
     */
    private void authenticateAndSaveToSession(String email, String password, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManager.authenticate(token);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션에 SecurityContext를 저장해서 다음 요청에서 자동 복원되게 한다
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
