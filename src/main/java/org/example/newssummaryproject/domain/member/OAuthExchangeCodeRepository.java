package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, Long> {

    Optional<OAuthExchangeCode> findByCodeHash(String codeHash);

    /**
     * 만료된 OAuth 1회용 코드를 일괄 삭제한다.
     * 코드는 60초 만료 + 1회 사용이라 만료 후엔 보존할 이유가 없다.
     */
    @Modifying
    @Query("DELETE FROM OAuthExchangeCode c WHERE c.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
