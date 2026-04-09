package org.example.newssummaryproject.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean oauth2Enabled = clientRegistrations.getIfAvailable() != null;

        http
                .csrf(csrf -> csrf.disable())
                // OAuth2 로그인 플로우에서 authorization request를 세션에 임시 저장해야 하므로
                // OAuth2가 활성화되면 IF_REQUIRED, 아니면 STATELESS
                .sessionManagement(session -> session
                        .sessionCreationPolicy(oauth2Enabled
                                ? SessionCreationPolicy.IF_REQUIRED
                                : SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/members/signup", "/api/members/login", "/api/members/refresh", "/api/members/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                        .requestMatchers("/", "/detail/**", "/mypage").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/react/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String json = "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\"}";
                            response.getWriter().write(json);
                        })
                )
                .logout(logout -> logout.disable());

        if (oauth2Enabled) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureUrl("/?error=oauth2")
            );
        }

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
