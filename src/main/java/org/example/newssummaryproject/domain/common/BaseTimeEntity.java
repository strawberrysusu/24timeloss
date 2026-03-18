package org.example.newssummaryproject.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 여러 엔티티가 공통으로 사용하는 시간 정보 부모 클래스다.
 *
 * 이 클래스를 상속하면 각 테이블에 created_at, updated_at 성격의 컬럼이 생기고
 * 저장/수정 시점이 자동으로 기록된다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    // 처음 DB에 저장된 시각
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막으로 수정된 시각
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
