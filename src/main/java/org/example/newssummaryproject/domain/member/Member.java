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
 * 회원 정보를 담는 엔티티다.
 *
 * 로그인/회원가입/마이페이지의 기준이 되는 테이블이며
 * 이후 저장 기사, 관심 카테고리, 읽은 기록이 모두 이 회원을 기준으로 연결된다.
 */
@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 아이디로 사용할 이메일. 중복되면 안 된다.
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 현재는 단순 문자열이지만, 실제 로그인 기능을 만들 때는 반드시 암호화해서 저장해야 한다.
    @Column(nullable = false, length = 200)
    private String password;

    // 화면에 표시할 닉네임
    @Column(nullable = false, length = 30)
    private String nickname;

    @Builder
    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
