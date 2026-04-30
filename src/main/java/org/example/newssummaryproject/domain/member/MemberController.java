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

import java.util.Map;

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
    // /api/members 하위 모든 엔드포인트(/refresh, /logout)에서 쿠키를 받기 위해 path를 좁히되 logout까지 포함시킨다.
    // /refresh로만 좁히면 logout 요청 때 브라우저가 쿠키를 보내지 않아 서버가 토큰을 폐기하지 못한다.
    private static final String REFRESH_COOKIE_PATH = "/api/members";

    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuthCodeExchangeService oauthCodeExchangeService;
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

        refreshTokenService.register(member.id(), refreshToken);
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

        refreshTokenService.register(member.id(), refreshToken);
        addRefreshCookie(response, refreshToken);

        return ResponseEntity.ok(LoginResponse.of(accessToken, member));
    }

    /**
     * 토큰 재발급 — POST /api/members/refresh
     *
     * 1. JWT 서명/형식 검증 (JwtProvider)
     * 2. DB에 등록된 활성 토큰인지 검증 + 즉시 폐기 (RefreshTokenService)
     *    - 이미 폐기된 토큰이 다시 들어오면 → 탈취 의심 → 해당 회원 모든 토큰 일괄 폐기
     * 3. 새 access + refresh 발급, 새 refresh도 DB에 등록
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || !jwtProvider.isValidRefreshToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long memberId = refreshTokenService.validateAndRevoke(refreshToken);

        MemberResponse member = memberService.getMember(memberId);
        String newAccessToken = jwtProvider.createAccessToken(memberId, member.email());
        String newRefreshToken = jwtProvider.createRefreshToken(memberId, member.email());

        refreshTokenService.register(memberId, newRefreshToken);
        addRefreshCookie(response, newRefreshToken);

        return ResponseEntity.ok(LoginResponse.of(newAccessToken, member));
    }

    /**
     * 로그아웃 — POST /api/members/logout
     *
     * 1. DB에서 해당 refresh token 폐기 (서버 측 무효화)
     * 2. httpOnly 쿠키를 maxAge=0으로 덮어써서 브라우저에서도 삭제
     * → access token이 만료되면 재발급 불가 → 사실상 세션 종료.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        refreshTokenService.revoke(refreshToken);

        boolean secure = isProd();
        String sameSite = secure ? "Strict" : "Lax";
        response.addHeader("Set-Cookie",
                REFRESH_COOKIE_NAME + "="
                + "; HttpOnly" + (secure ? "; Secure" : "")
                + "; Path=" + REFRESH_COOKIE_PATH
                + "; Max-Age=0"
                + "; SameSite=" + sameSite);
        return ResponseEntity.ok().build();
    }

    /**
     * OAuth 1회용 코드 → access token 교환 — POST /api/members/oauth/exchange
     *
     * OAuth2LoginSuccessHandler가 발급한 oauth_code를 access token + refresh cookie로 교환한다.
     * code는 1회용이고 60초 후 만료된다.
     */
    @PostMapping("/oauth/exchange")
    public ResponseEntity<LoginResponse> exchangeOAuthCode(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        String code = body == null ? null : body.get("code");
        Long memberId = oauthCodeExchangeService.redeem(code);

        MemberResponse member = memberService.getMember(memberId);
        String accessToken = jwtProvider.createAccessToken(member.id(), member.email());
        String refreshToken = jwtProvider.createRefreshToken(member.id(), member.email());

        refreshTokenService.register(member.id(), refreshToken);
        addRefreshCookie(response, refreshToken);

        return ResponseEntity.ok(LoginResponse.of(accessToken, member));
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
     * - path: /api/members 로 한정 → /refresh, /logout 모두에 자동 전송, 다른 API에는 불필요하게 붙지 않음
     * - SameSite: prod=Strict (CSRF 방어), dev=Lax (로컬 개발 편의)
     */
    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        boolean secure = isProd();
        String sameSite = secure ? "Strict" : "Lax";
        long maxAge = refreshExpirationDays * 24 * 60 * 60;

        response.addHeader("Set-Cookie",
                REFRESH_COOKIE_NAME + "=" + refreshToken
                + "; HttpOnly" + (secure ? "; Secure" : "")
                + "; Path=" + REFRESH_COOKIE_PATH
                + "; Max-Age=" + maxAge
                + "; SameSite=" + sameSite);
    }

    private boolean isProd() {
        return environment.acceptsProfiles(Profiles.of("prod"));
    }
}
