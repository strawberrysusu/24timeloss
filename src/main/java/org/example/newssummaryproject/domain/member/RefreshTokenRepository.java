package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 회원의 모든 활성 토큰을 일괄 폐기한다.
     * 재사용 탐지(이미 revoked된 토큰이 다시 들어옴) 시 호출.
     *
     * clearAutomatically=true: UPDATE 후 영속성 컨텍스트를 비워서
     * 후속 findByTokenHash가 stale 데이터를 반환하지 않도록 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now " +
            "WHERE t.memberId = :memberId AND t.revokedAt IS NULL")
    int revokeAllForMember(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /**
     * 만료된 토큰 또는 폐기된 지 일정 기간 지난 토큰을 일괄 삭제한다.
     *
     * - 만료 토큰: now 기준으로 즉시 삭제 (이미 사용 불가 상태)
     * - 폐기 토큰: 재사용 탐지 신호로 쓰일 수 있어 revokedCutoff(예: 14일 전) 이후 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revokedAt < :revokedCutoff")
    int deleteExpiredOrOldRevoked(
            @Param("now") LocalDateTime now,
            @Param("revokedCutoff") LocalDateTime revokedCutoff);
}
