package org.example.newssummaryproject.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Member 테이블에 접근하는 Repository다.
 *
 * 일반적인 흐름은 Controller -> Service -> Repository -> DB 이다.
 * 지금은 Service 계층이 아직 없지만, 이후 회원가입/로그인 기능에서 사용하게 된다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이메일로 회원 존재 여부를 확인하거나 로그인 시 회원을 찾을 때 사용
    Optional<Member> findByEmail(String email);

    // 회원가입 시 이메일 중복 체크
    boolean existsByEmail(String email);
}
