package org.example.newssummaryproject.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.MemberService;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Google OAuth2 로그인 성공 시 JWT를 발급하고 프론트엔드로 리다이렉트한다.
 *
 * 흐름:
 *   1. Google 인증 완료 → Spring Security가 이 핸들러를 호출
 *   2. Google 사용자 정보(email, name, sub)로 회원 찾기/생성
 *   3. JWT Access Token + Refresh Token(httpOnly 쿠키) 발급
 *   4. /?token={accessToken} 으로 리다이렉트 → 프론트가 토큰 저장
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final Environment environment;

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        MemberResponse member = memberService.findOrCreateByGoogle(email, name, googleId);

        String accessToken = jwtProvider.createAccessToken(member.id(), member.email());
        String refreshToken = jwtProvider.createRefreshToken(member.id(), member.email());

        addRefreshCookie(response, refreshToken);

        getRedirectStrategy().sendRedirect(request, response, "/?token=" + accessToken);
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        boolean secure = environment.acceptsProfiles(Profiles.of("prod"));
        String sameSite = secure ? "Strict" : "Lax";
        long maxAge = refreshExpirationDays * 24 * 60 * 60;

        response.addHeader("Set-Cookie",
                "refresh_token=" + refreshToken
                + "; HttpOnly" + (secure ? "; Secure" : "")
                + "; Path=/api/members/refresh"
                + "; Max-Age=" + maxAge
                + "; SameSite=" + sameSite);
    }
}
