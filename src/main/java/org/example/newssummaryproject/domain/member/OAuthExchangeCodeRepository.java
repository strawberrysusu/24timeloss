package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, Long> {

    Optional<OAuthExchangeCode> findByCodeHash(String codeHash);
}
