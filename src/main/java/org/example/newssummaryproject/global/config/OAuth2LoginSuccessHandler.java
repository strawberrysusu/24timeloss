package org.example.newssummaryproject.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.MemberService;
import org.example.newssummaryproject.domain.member.OAuthCodeExchangeService;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 1회용 교환 코드를 발급하고 프론트엔드로 리다이렉트한다.
 *
 * 흐름:
 *   1. Google/Naver 인증 완료 → Spring Security가 이 핸들러 호출
 *   2. 사용자 정보로 회원 찾기/생성
 *   3. 1회용 oauth_code 발급 (DB에 해시 저장, 60초 수명)
 *   4. /?oauth_code={code} 로 리다이렉트
 *   5. 프론트가 POST /api/members/oauth/exchange { code } 호출 → access + refresh 발급
 *
 * 왜 access token을 직접 URL로 안 넘기나?
 *   URL query는 서버 access log, 프록시 log, 브라우저 history, referrer 헤더에 기록될 수 있음.
 *   1분 이내 1회용 code는 노출돼도 즉시 무효화 + 만료되어 위험이 작음.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final MemberService memberService;
    private final OAuthCodeExchangeService oauthCodeExchangeService;

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

        String exchangeCode = oauthCodeExchangeService.issue(member.id());
        getRedirectStrategy().sendRedirect(request, response, "/?oauth_code=" + exchangeCode);
    }

    private String getString(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value instanceof String stringValue ? stringValue : null;
    }
}
