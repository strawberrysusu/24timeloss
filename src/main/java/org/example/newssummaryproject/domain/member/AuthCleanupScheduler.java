package org.example.newssummaryproject.domain.member;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 관련 임시 데이터(refresh_tokens, oauth_exchange_codes)를 주기적으로 정리하는 스케줄러.
 *
 * 두 테이블 모두 발급 → 회전/소비 → 폐기/만료 흐름이라 시간이 지나면 사용 불가능한 행만 쌓인다.
 * 그대로 두면 테이블이 무한히 커져 SELECT/INDEX 비용이 늘어나므로 정기적으로 삭제한다.
 *
 * - refresh_tokens:
 *     · 만료된 토큰(expires_at < now)은 즉시 삭제 (이미 사용 불가).
 *     · 폐기된 토큰은 재사용 탐지 신호로 쓰일 수 있어 14일 보존 후 삭제.
 * - oauth_exchange_codes: 60초 1회용 코드라 만료된 행은 즉시 삭제해도 안전하다.
 */
@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuthCleanupScheduler.class);
    private static final int REVOKED_RETENTION_DAYS = 14;

    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthExchangeCodeRepository oauthExchangeCodeRepository;

    // 매일 새벽 4시(KST) 실행 — 트래픽 적은 시간대에 정리해 부하 영향 최소화.
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime revokedCutoff = now.minusDays(REVOKED_RETENTION_DAYS);

        int tokens = refreshTokenRepository.deleteExpiredOrOldRevoked(now, revokedCutoff);
        int codes = oauthExchangeCodeRepository.deleteExpired(now);

        log.info("인증 데이터 정리 완료: refresh_tokens={}건, oauth_exchange_codes={}건", tokens, codes);
    }
}
