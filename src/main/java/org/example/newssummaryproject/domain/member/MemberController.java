package org.example.newssummaryproject.domain.member;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.LoginRequest;
import org.example.newssummaryproject.domain.member.dto.LoginResponse;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.example.newssummaryproject.domain.member.dto.SignupRequest;
import org.example.newssummaryproject.global.config.CustomUserDetails;
import org.example.newssummaryproject.global.config.JwtProvider;
import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * ── 회원 컨트롤러 ──
 *
 * 인증 방식 — JWT (Access + Refresh):
 *   - Access Token: JSON 응답으로 전달 → 프론트가 메모리/localStorage에 보관 → Authorization 헤더로 전송
 *   - Refresh Token: httpOnly 쿠키로 전달 → JS에서 접근 불가 (XSS 방어) → 브라우저가 자동 전송
 *
 * 로그아웃: 서버에서 refresh 쿠키를 삭제(maxAge=0)하면 재발급 불가 → 사실상 세션 종료
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final Environment environment;

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    /**
     * 회원가입 — POST /api/members/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response) {
        MemberResponse member = memberService.signup(request);
        String accessToken = jwtProvider.createAccessToken(member.id(), member.email());
        String refreshToken = jwtProvider.createRefreshToken(member.id(), member.email());

        addRefreshCookie(response, refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LoginResponse.of(accessToken, member));
    }

    /**
     * 로그인 — POST /api/members/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        MemberResponse member = memberService.getMemberByEmail(request.email());
        String accessToken = jwtProvider.createAccessToken(member.id(), member.email());
        String refreshToken = jwtProvider.createRefreshToken(member.id(), member.email());

        addRefreshCookie(response, refreshToken);

        return ResponseEntity.ok(LoginResponse.of(accessToken, member));
    }

    /**
     * 토큰 재발급 — POST /api/members/refresh
     *
     * httpOnly 쿠키에서 refresh token을 읽어서 새 access + refresh를 발급한다.
     * Refresh Token Rotation: 재발급 시 refresh도 새로 교체한다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || !jwtProvider.isValidRefreshToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long memberId = jwtProvider.getMemberId(refreshToken);
        String email = jwtProvider.getEmail(refreshToken);

        String newAccessToken = jwtProvider.createAccessToken(memberId, email);
        String newRefreshToken = jwtProvider.createRefreshToken(memberId, email);

        addRefreshCookie(response, newRefreshToken);

        MemberResponse member = memberService.getMember(memberId);
        return ResponseEntity.ok(LoginResponse.of(newAccessToken, member));
    }

    /**
     * 로그아웃 — POST /api/members/logout
     *
     * httpOnly 쿠키를 maxAge=0으로 덮어써서 브라우저에서 삭제한다.
     * refresh token이 사라지면 access token이 만료된 후 재발급이 불가능하다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        boolean secure = isProd();
        String sameSite = secure ? "Strict" : "Lax";
        response.addHeader("Set-Cookie",
                REFRESH_COOKIE_NAME + "="
                + "; HttpOnly" + (secure ? "; Secure" : "")
                + "; Path=/api/members/refresh"
                + "; Max-Age=0"
                + "; SameSite=" + sameSite);
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

    public static Long getLoginMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getMemberId();
    }

    public static Long getLoginMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getMemberId();
    }

    /**
     * Refresh Token을 httpOnly 쿠키로 설정한다.
     *
     * - httpOnly: JS에서 document.cookie로 접근 불가 → XSS 방어
     * - secure: prod에서만 true (HTTPS 필수). dev/test에서는 false (HTTP 허용)
     * - path: /api/members/refresh 로만 전송 → 다른 API에 불필요하게 붙지 않음
     * - SameSite: prod=Strict (CSRF 방어), dev=Lax (로컬 개발 편의)
     */
    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        boolean secure = isProd();
        String sameSite = secure ? "Strict" : "Lax";
        long maxAge = refreshExpirationDays * 24 * 60 * 60;

        response.addHeader("Set-Cookie",
                REFRESH_COOKIE_NAME + "=" + refreshToken
                + "; HttpOnly" + (secure ? "; Secure" : "")
                + "; Path=/api/members/refresh"
                + "; Max-Age=" + maxAge
                + "; SameSite=" + sameSite);
    }

    private boolean isProd() {
        return environment.acceptsProfiles(Profiles.of("prod"));
    }
}
