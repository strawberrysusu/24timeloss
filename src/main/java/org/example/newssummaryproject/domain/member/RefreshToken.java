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
 * 발급된 refresh token의 해시를 저장한다.
 *
 * raw token이 아닌 SHA-256 해시를 저장하는 이유:
 *   DB가 유출돼도 활성 토큰을 그대로 사용할 수 없게 하기 위함.
 *   토큰 검증 시: 클라이언트가 보낸 raw token을 해시해서 DB에서 찾는다.
 *
 * 토큰 상태:
 *   - revokedAt == null AND expiresAt > now : 유효
 *   - revokedAt != null : 폐기됨 (재사용 시 탈취 의심)
 *   - expiresAt <= now : 만료됨
 */
@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** SHA-256 hex 64자. unique. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 폐기되면 시각 기록. 한 번 폐기된 토큰은 다시 사용 불가. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    private RefreshToken(Long memberId, String tokenHash, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now();
        }
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isExpired() {
        return this.expiresAt.isBefore(LocalDateTime.now());
    }
}
