package org.example.newssummaryproject.global.config;

import lombok.Getter;
import org.example.newssummaryproject.domain.member.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security가 인증된 사용자 정보를 담는 객체다.
 *
 * Member 엔티티를 감싸서 Spring Security가 요구하는 UserDetails 인터페이스를 구현한다.
 * SecurityContextHolder에서 꺼내면 getMemberId()로 회원 ID를 바로 얻을 수 있다.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String email;
    private final String password;

    public CustomUserDetails(Member member) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.password = member.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
