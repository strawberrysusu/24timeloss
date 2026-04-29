package org.example.newssummaryproject.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.newssummaryproject.domain.common.BaseTimeEntity;

import java.time.LocalDateTime;

/**
 * OAuth 로그인 성공 후 프론트가 access token을 안전하게 받기 위한 1회용 교환 코드.
 *
 * raw code가 아닌 SHA-256 해시만 저장한다 (DB 유출 시 안전).
 * 한 번 사용하면 redeemedAt이 채워져서 재사용 불가.
 */
@Getter
@Entity
@Table(name = "oauth_exchange_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthExchangeCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @Builder
    private OAuthExchangeCode(Long memberId, String codeHash, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public void redeem() {
        if (this.redeemedAt == null) {
            this.redeemedAt = LocalDateTime.now();
        }
    }

    public boolean isRedeemed() {
        return this.redeemedAt != null;
    }

    public boolean isExpired() {
        return this.expiresAt.isBefore(LocalDateTime.now());
    }
}
