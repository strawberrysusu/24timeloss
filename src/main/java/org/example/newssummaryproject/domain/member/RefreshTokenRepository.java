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
}
