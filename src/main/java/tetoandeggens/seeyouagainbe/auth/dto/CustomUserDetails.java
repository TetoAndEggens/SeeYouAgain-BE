package tetoandeggens.seeyouagainbe.auth.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String password;
    private final String role;

    // 일반 로그인용 생성자
    public CustomUserDetails(Member member) {
        this.userId = member.getId();
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.role = member.getRole().getRole();
    }

    // JWT Claims에서 복원용 생성자
    private CustomUserDetails(Long userId, String loginId, String password, String role) {
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
    }

    // JWT Claims에서 CustomUserDetails 생성
    public static CustomUserDetails fromClaims(String userId, String role) {
        return new CustomUserDetails(
                Long.parseLong(userId),
                null,
                null,
                role
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
