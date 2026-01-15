package tetoandeggens.seeyouagainbe.auth.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.entity.Role;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final String uuid;
    private final Long memberId;
    private final String loginId;
    private final String password;
    private final Role role;
    private final Boolean isBanned;

    public CustomUserDetails(Member member) {
        this.uuid = member.getUuid();
        this.memberId = member.getId();
        this.loginId = member.getLoginId();
        this.password = member.getPassword();
        this.role = member.getRole();
        this.isBanned = member.getIsBanned();
    }

    public static CustomUserDetails fromClaims(String uuid, String role, Long memberId) {
        String roleValue = role.startsWith("ROLE_") ? role.substring(5) : role;
        return new CustomUserDetails(uuid, null, null, Role.valueOf(roleValue), memberId, false);
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

    private CustomUserDetails(String uuid, String loginId, String password, Role role, Long memberId, Boolean isBanned) {
        this.memberId = memberId;
        this.uuid = uuid;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.isBanned = isBanned;
    }
}
