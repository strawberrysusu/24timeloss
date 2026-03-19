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

/**
 * 회원 정보를 담는 엔티티(Entity)다.
 *
 * 엔티티란? DB 테이블과 1:1로 매핑되는 자바 클래스다.
 * 이 클래스의 필드 하나하나가 DB 테이블의 컬럼이 된다.
 *
 * 연결 관계:
 *   members (1) ──── (N) saved_articles      → 저장한 기사
 *   members (1) ──── (N) member_interests     → 관심 카테고리
 *   members (1) ──── (N) article_read_histories → 읽기 기록
 *   members (1) ──── (N) articles (writer_id)  → 작성한 기사
 *
 * 핵심 어노테이션:
 *   - @Entity: "이 클래스는 DB 테이블과 매핑된다"고 JPA에 알린다.
 *   - @Table(name = "members"): DB에서 실제 테이블 이름을 "members"로 지정한다.
 *   - @Getter: Lombok이 모든 필드의 getter 메서드를 자동 생성한다.
 *   - @NoArgsConstructor(access = PROTECTED): JPA가 내부적으로 리플렉션으로
 *              객체를 만들 때 쓰는 기본 생성자. PROTECTED로 외부에서 직접 못 쓰게 막는다.
 *              → 객체 생성은 Builder 패턴(@Builder)을 통해서만 하도록 유도한다.
 *   - extends BaseTimeEntity: createdAt, updatedAt 필드를 상속받는다.
 */
@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    // @Id: 이 필드가 테이블의 기본키(PK)임을 나타낸다.
    // @GeneratedValue(IDENTITY): DB가 AUTO_INCREMENT로 ID를 자동 채번한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 아이디로 사용할 이메일. unique = true로 DB 레벨에서 중복을 방지한다.
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // BCrypt로 암호화된 비밀번호가 저장된다. (MemberService.signup에서 암호화 처리)
    // 예: "1234" → "$2a$10$xK8r..." 형태. length=200은 해시 문자열 길이를 위한 것.
    @Column(nullable = false, length = 200)
    private String password;

    // 화면에 표시할 닉네임
    @Column(nullable = false, length = 30)
    private String nickname;

    // @Builder: Lombok이 빌더 패턴 코드를 자동 생성한다.
    // 사용법: Member.builder().email("a@b.com").password("...").nickname("홍길동").build()
    // private 접근자로 외부에서 직접 new Member()를 못 쓰게 하고, Builder만 허용한다.
    @Builder
    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    // 닉네임 변경 — @Transactional 안에서 호출하면 dirty checking으로 자동 UPDATE된다
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 비밀번호 변경 — 반드시 암호화된(encoded) 비밀번호를 파라미터로 받아야 한다
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
