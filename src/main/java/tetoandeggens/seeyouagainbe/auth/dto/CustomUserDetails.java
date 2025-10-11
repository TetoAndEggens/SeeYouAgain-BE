package tetoandeggens.seeyouagainbe.auth.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.entity.Role;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final String uuid;
    private final String loginId;
    private final String password;
    private final Role role;

    // 일반 로그인용 생성자
    public CustomUserDetails(Member member) {
        this.uuid = member.getUuid();
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.role = member.getRole();
    }

    // JWT Claims에서 CustomUserDetails 생성
    public static CustomUserDetails fromClaims(String uuid, String roleStr) {
        Role role = Role.fromString(roleStr);
        return new CustomUserDetails(uuid, null, null, role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getRole()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return uuid;
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

    // JWT Claims에서 복원용 생성자
    private CustomUserDetails(String uuid, String loginId, String password, Role role) {
        this.uuid = uuid;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
    }
}
