package org.example.newssummaryproject.domain.member;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.example.newssummaryproject.domain.member.dto.SignupRequest;
import org.example.newssummaryproject.global.exception.DuplicateException;
import org.example.newssummaryproject.global.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * ── 회원 서비스 (비즈니스 로직 계층) ──
 *
 * 흐름: Controller → ★ Service → Repository
 *
 * Controller가 HTTP 요청을 받으면 이 Service를 호출하고,
 * Service가 비즈니스 규칙(중복 검사, 비밀번호 확인 등)을 처리한 뒤
 * Repository를 통해 DB와 소통한다.
 *
 * 핵심 개념:
 *   - @Service: 이 클래스가 비즈니스 로직 담당임을 Spring에 알린다.
 *              Spring이 자동으로 객체(Bean)를 만들어 관리해 준다.
 *   - @RequiredArgsConstructor: final 필드를 파라미터로 받는 생성자를 Lombok이 자동 생성.
 *              Spring이 이 생성자로 MemberRepository, PasswordEncoder를 주입(DI)한다.
 *   - @Transactional(readOnly = true): 클래스 전체 기본을 "읽기 전용 트랜잭션"으로 설정.
 *              DB를 변경하는 메서드에만 @Transactional을 따로 붙여 쓰기 모드로 전환한다.
 *              읽기 전용이면 JPA가 변경 감지(dirty checking)를 건너뛰어 성능이 좋아진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    // Spring이 생성자 주입(Constructor Injection)으로 아래 두 객체를 자동으로 넣어준다.
    private final MemberRepository memberRepository;   // DB 조회/저장 담당
    private final PasswordEncoder passwordEncoder;      // BCrypt 암호화 담당

    /**
     * 회원가입을 처리한다.
     *
     * 처리 흐름:
     *   1. 이메일 중복 검사 → 이미 있으면 409(DUPLICATE) 에러
     *   2. 비밀번호를 BCrypt로 암호화 (평문 저장은 보안상 절대 안 된다)
     *   3. Member 엔티티를 만들어 DB에 저장
     *   4. MemberResponse DTO로 변환해서 반환 (비밀번호 제외)
     *
     * @Transactional: DB에 새 데이터를 INSERT하므로 쓰기 모드가 필요하다.
     */
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 이메일 중복 검사 — 같은 이메일로 두 번 가입하면 안 된다
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateException("이미 사용 중인 이메일입니다.");
        }

        // Builder 패턴으로 Member 생성. passwordEncoder.encode()가 BCrypt 해시 처리를 한다.
        // 예: "1234" → "$2a$10$xK8r..." 이런 식으로 바뀐다. 원본은 복원 불가.
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        // memberRepository.save()가 DB에 INSERT하고, 자동 생성된 id가 member에 세팅된다.
        return MemberResponse.from(memberRepository.save(member));
    }

    /**
     * 이메일로 회원 정보를 조회한다.
     * 로그인 성공 후 응답 데이터를 만들 때 사용한다.
     */
    public MemberResponse getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        return MemberResponse.from(member);
    }

    /**
     * 회원 정보를 ID로 조회한다.
     * /api/members/me 엔드포인트에서 현재 로그인한 사용자 정보를 가져올 때 사용한다.
     */
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        return MemberResponse.from(member);
    }

    /**
     * 닉네임을 변경한다.
     *
     * member.updateNickname() 호출 후 별도 save() 없이도 DB가 업데이트된다.
     * 이유: @Transactional 안에서 JPA가 "dirty checking"을 하기 때문이다.
     * → 트랜잭션이 끝날 때 JPA가 엔티티의 변경 사항을 감지해서 자동으로 UPDATE SQL을 실행한다.
     */
    @Transactional
    public MemberResponse updateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        member.updateNickname(nickname);
        return MemberResponse.from(member);
    }

    /**
     * 비밀번호를 변경한다. 현재 비밀번호 확인 후 새 비밀번호로 변경한다.
     *
     * 반환형이 void인 이유: 비밀번호는 응답에 포함시키면 안 되고,
     * 성공하면 200 OK만 보내면 충분하기 때문이다.
     */
    @Transactional
    public void updatePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        // 현재 비밀번호가 맞는지 먼저 확인 — 남이 비밀번호를 바꾸는 것을 방지
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 새 비밀번호도 반드시 BCrypt로 암호화해서 저장한다
        member.updatePassword(passwordEncoder.encode(newPassword));
    }
}
