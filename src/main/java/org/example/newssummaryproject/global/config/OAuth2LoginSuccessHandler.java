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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

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
        String registrationId = authentication instanceof OAuth2AuthenticationToken oauthToken
                ? oauthToken.getAuthorizedClientRegistrationId()
                : "google";

        MemberResponse member;
        try {
            member = switch (registrationId) {
                case "naver" -> {
                    Map<String, Object> responseAttributes = oauth2User.getAttribute("response");
                    String email = getString(responseAttributes, "email");
                    String name = getString(responseAttributes, "name");
                    String naverId = getString(responseAttributes, "id");
                    yield memberService.findOrCreateByNaver(email, name, naverId);
                }
                case "google" -> {
                    String email = oauth2User.getAttribute("email");
                    String name = oauth2User.getAttribute("name");
                    String googleId = oauth2User.getAttribute("sub");
                    yield memberService.findOrCreateByGoogle(email, name, googleId);
                }
                default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
            };
        } catch (IllegalStateException e) {
            // 네이버 이메일 미동의 등 → 사용자에게 친절한 에러로 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, "/?oauth_error=" + e.getMessage());
            return;
        }

        String accessToken = jwtProvider.createAccessToken(member.id(), member.email());
        String refreshToken = jwtProvider.createRefreshToken(member.id(), member.email());

        addRefreshCookie(response, refreshToken);

        getRedirectStrategy().sendRedirect(request, response, "/?token=" + accessToken);
    }

    private String getString(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value instanceof String stringValue ? stringValue : null;
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
