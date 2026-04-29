package org.example.newssummaryproject.domain.member;

import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OAuthCodeExchangeServiceTest {

    @Autowired
    OAuthCodeExchangeService service;

    @Autowired
    MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member m = memberRepository.save(Member.builder()
                .email("oauth-test@test.com")
                .password("x")
                .nickname("oauth-tester")
                .build());
        memberId = m.getId();
    }

    @Test
    void issue_then_redeem_returns_member_id() {
        String code = service.issue(memberId);
        Long redeemed = service.redeem(code);
        assertThat(redeemed).isEqualTo(memberId);
    }

    @Test
    void redeeming_same_code_twice_fails() {
        String code = service.issue(memberId);
        service.redeem(code);

        assertThatThrownBy(() -> service.redeem(code))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("이미 사용");
    }

    @Test
    void redeem_rejects_unknown_code() {
        assertThatThrownBy(() -> service.redeem("unknowncode1234567890"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void redeem_rejects_blank_code() {
        assertThatThrownBy(() -> service.redeem(""))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.redeem(null))
                .isInstanceOf(UnauthorizedException.class);
    }
}
