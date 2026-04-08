package org.example.newssummaryproject.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정이다.
 *
 * 핵심 역할:
 *   1. URL별 접근 권한 설정 (누가 어떤 API에 접근 가능한지)
 *   2. CSRF 비활성화 (REST API이므로 브라우저 폼 방어가 불필요)
 *   3. 비로그인 상태에서 보호된 URL 접근 시 JSON 401 응답
 *   4. PasswordEncoder, AuthenticationManager 빈 등록
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 회원가입, 로그인은 누구나 접근 가능
                .requestMatchers("/api/members/signup", "/api/members/login").permitAll()

                // 기사 조회 API는 누구나 접근 가능 (GET)
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()

                // 트렌딩, 브리핑, 인기 검색어, 추천은 GET이므로 위에서 이미 허용됨

                // 페이지 뷰 (Mustache)
                .requestMatchers("/", "/detail/**", "/mypage").permitAll()

                // 정적 리소스
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                // Swagger (개발 환경)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // 헬스체크
                .requestMatchers("/health").permitAll()

                // 나머지 모든 요청은 로그인 필수
                .anyRequest().authenticated()
            )
            // 비로그인 상태에서 보호된 API 접근 시 JSON 401 응답
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    String json = "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\"}";
                    response.getWriter().write(json);
                })
            )
            // 로그아웃은 MemberController에서 직접 처리하므로 기본 로그아웃 비활성화
            .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
