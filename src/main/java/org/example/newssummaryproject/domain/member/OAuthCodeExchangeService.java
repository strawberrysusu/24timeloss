package org.example.newssummaryproject.domain.member;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OAuth 로그인 성공 후 프론트가 access token을 받기 위한 1회용 교환 코드를 관리한다.
 *
 * 흐름:
 *   1. OAuth2LoginSuccessHandler가 issue(memberId)로 raw code 발급
 *   2. /?oauth_code=xxx 로 리다이렉트
 *   3. 프론트가 POST /api/members/oauth/exchange { code } 호출
 *   4. redeem(code)로 검증 + 1회용 처리 + memberId 반환
 *   5. 컨트롤러가 access + refresh 발급
 *
 * 보안:
 *   - code는 32바이트 SecureRandom (64자 hex)
 *   - DB에는 SHA-256 해시만 저장
 *   - 60초 수명 (URL/로그/history에 남아있어도 곧 만료)
 *   - 1회 사용 후 redeemed_at 기록 → 재사용 불가
 */
@Service
@RequiredArgsConstructor
public class OAuthCodeExchangeService {

    private static final int CODE_TTL_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthExchangeCodeRepository repository;

    /**
     * 새 1회용 코드를 발급한다.
     * 반환되는 raw code는 DB에 저장되지 않고, 해시만 저장된다.
     */
    @Transactional
    public String issue(Long memberId) {
        String rawCode = generateRandomCode();
        String hash = sha256Hex(rawCode);
        repository.save(OAuthExchangeCode.builder()
                .memberId(memberId)
                .codeHash(hash)
                .expiresAt(LocalDateTime.now().plusSeconds(CODE_TTL_SECONDS))
                .build());
        return rawCode;
    }

    /**
     * 코드를 검증하고 1회용 처리한다. 호출 측은 반환된 memberId로 토큰을 발급한다.
     *
     * 실패 케이스:
     *   - code가 빈 값
     *   - DB에 없음 (위조/이미 만료된 후 정리됨)
     *   - 이미 redeemed (재사용 시도)
     *   - 만료됨
     */
    @Transactional
    public Long redeem(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new UnauthorizedException("교환 코드가 비어있습니다.");
        }
        String hash = sha256Hex(rawCode);
        OAuthExchangeCode stored = repository.findByCodeHash(hash)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 교환 코드입니다."));

        if (stored.isRedeemed()) {
            throw new UnauthorizedException("이미 사용된 교환 코드입니다.");
        }
        if (stored.isExpired()) {
            throw new UnauthorizedException("만료된 교환 코드입니다.");
        }

        stored.redeem();
        return stored.getMemberId();
    }

    private static String generateRandomCode() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

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
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
