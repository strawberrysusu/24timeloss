package org.example.newssummaryproject.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성/검증/파싱을 담당한다.
 *
 * 토큰 종류:
 *   - Access Token: 짧은 수명 (기본 1시간), API 호출에 사용
 *   - Refresh Token: 긴 수명 (기본 7일), access token 재발급에 사용
 *
 * 클라이언트 흐름:
 *   1. 로그인 → access + refresh 토큰 수령
 *   2. API 호출 → Authorization: Bearer {accessToken}
 *   3. access 만료 → POST /api/members/refresh + refresh 토큰으로 재발급
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
    private static final String DEV_SECRET_PREFIX = "defaultDevSecret";

    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-hours:1}") long accessExpirationHours,
            @Value("${jwt.refresh-expiration-days:7}") long refreshExpirationDays,
            Environment environment) {
        // prod 환경에서 기본 dev 시크릿을 사용하면 앱 시작을 차단한다
        if (environment.acceptsProfiles(Profiles.of("prod")) && secret.startsWith(DEV_SECRET_PREFIX)) {
            throw new IllegalStateException(
                    "운영 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다. " +
                    "기본 개발용 시크릿으로는 시작할 수 없습니다.");
        }
        if (secret.startsWith(DEV_SECRET_PREFIX)) {
            log.warn("⚠ 개발용 기본 JWT 시크릿을 사용 중입니다. 운영 환경에서는 JWT_SECRET을 반드시 설정하세요.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = accessExpirationHours * 60 * 60 * 1000;
        this.refreshExpirationMillis = refreshExpirationDays * 24 * 60 * 60 * 1000;
    }

    /**
     * Access Token을 생성한다. (짧은 수명)
     */
    public String createAccessToken(Long memberId, String email) {
        return createToken(memberId, email, "access", accessExpirationMillis);
    }

    /**
     * Refresh Token을 생성한다. (긴 수명)
     */
    public String createRefreshToken(Long memberId, String email) {
        return createToken(memberId, email, "refresh", refreshExpirationMillis);
    }

    private String createToken(Long memberId, String email, String type, long expirationMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("email", email)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String getType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /**
     * Access Token인지 검증한다. (type이 "access"이고 유효해야 함)
     */
    public boolean isValidAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "access".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Refresh Token인지 검증한다. (type이 "refresh"이고 유효해야 함)
     */
    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
