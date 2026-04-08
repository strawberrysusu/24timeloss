package org.example.newssummaryproject.global.config;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.Member;
import org.example.newssummaryproject.domain.member.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security가 로그인할 때 DB에서 사용자를 찾는 서비스다.
 *
 * AuthenticationManager가 내부적으로 이 클래스를 호출해서
 * 이메일로 Member를 조회하고 비밀번호를 비교한다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다."));
        return new CustomUserDetails(member);
    }
}
