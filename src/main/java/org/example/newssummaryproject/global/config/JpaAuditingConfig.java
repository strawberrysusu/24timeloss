package org.example.newssummaryproject.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 기능을 켜는 설정이다.
 *
 * 이 설정이 있어야 BaseTimeEntity의 createdAt, updatedAt 값이
 * 엔티티 저장/수정 시점에 자동으로 채워진다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
