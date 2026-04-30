package org.example.newssummaryproject.domain.member;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * Refresh token 발급/회전/폐기를 담당한다.
 *
 * 핵심 정책:
 *   - 발급된 토큰의 해시(SHA-256)만 DB에 저장한다 (raw 미저장 — 유출 시에도 안전).
 *   - /refresh 호출 시 기존 토큰을 즉시 revoke 처리하고 새 토큰을 발급한다 (rotation).
 *   - 이미 revoked된 토큰이 다시 들어오면 → 해당 회원의 모든 활성 토큰을 일괄 폐기한다 (탈취 의심).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    /**
     * 새 refresh token을 DB에 등록한다 (해시만 저장).
     */
    @Transactional
    public void register(Long memberId, String rawToken) {
        String hash = sha256Hex(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshExpirationDays);
        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(memberId)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .build());
    }

    /**
     * 클라이언트가 보낸 raw refresh token을 검증하고, 유효하면 즉시 폐기한다.
     * 호출 측은 반환된 memberId로 새 토큰을 발급해서 register()로 등록해야 한다.
     *
     * 실패 케이스:
     *   - DB에 없음 → 위조/만료 후 정리됨
     *   - 만료됨
     *   - 이미 revoked → 탈취 의심: 해당 회원 전체 토큰 폐기 후 거부
     */
    @Transactional
    public Long validateAndRevoke(String rawToken) {
        String hash = sha256Hex(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 리프레시 토큰입니다."));

        if (stored.isRevoked()) {
            // 한 번 폐기된 토큰이 다시 들어옴 = 탈취된 토큰을 누군가 재사용하는 정황.
            // 해당 회원의 모든 활성 토큰을 일괄 폐기해서 공격자/정상 사용자 모두 강제 로그아웃.
            int revoked = refreshTokenRepository.revokeAllForMember(
                    stored.getMemberId(), LocalDateTime.now());
            log.warn("Refresh token 재사용 탐지: memberId={}, 일괄 폐기 {}건", stored.getMemberId(), revoked);
            throw new UnauthorizedException("재사용 탐지되었습니다. 다시 로그인해주세요.");
        }

        if (stored.isExpired()) {
            throw new UnauthorizedException("만료된 리프레시 토큰입니다.");
        }

        stored.revoke();
        return stored.getMemberId();
    }

    /**
     * 로그아웃 시 호출. 토큰이 DB에 있으면 폐기한다.
     * 없거나 이미 폐기된 경우는 조용히 무시한다 (로그아웃은 항상 성공시켜야 UX 자연스러움).
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    /**
     * 회원의 모든 활성 토큰을 폐기한다.
     * 비밀번호 변경 등 보안 이벤트에서 호출된다 — 탈취된 세션의 잔존을 막는다.
     */
    @Transactional
    public void revokeAllForMember(Long memberId) {
        refreshTokenRepository.revokeAllForMember(memberId, LocalDateTime.now());
    }

    /**
     * SHA-256 해시를 64자 hex 문자열로 반환한다.
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JDK에 기본 포함 — 실제로는 발생 불가
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
