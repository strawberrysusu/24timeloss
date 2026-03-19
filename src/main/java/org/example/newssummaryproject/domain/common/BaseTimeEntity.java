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
 * Member, Article, ArticleSummary 등이 이 클래스를 상속(extends)한다.
 * → 모든 테이블에 created_at, updated_at 컬럼이 자동으로 생기고,
 *   INSERT/UPDATE 시점이 자동 기록된다.
 *
 * 핵심 어노테이션:
 *   - @MappedSuperclass: 이 클래스 자체는 테이블이 아니고,
 *              상속받는 엔티티의 테이블에 필드가 포함된다.
 *   - @EntityListeners(AuditingEntityListener.class):
 *              JPA Auditing 기능을 활성화한다.
 *              엔티티가 저장/수정될 때 Spring이 자동으로 시간을 넣어준다.
 *              (JpaConfig에서 @EnableJpaAuditing 설정이 필요하다)
 *   - @CreatedDate: INSERT 시 현재 시각이 자동 입력된다. updatable=false로 이후 변경 불가.
 *   - @LastModifiedDate: UPDATE 때마다 현재 시각으로 자동 갱신된다.
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
