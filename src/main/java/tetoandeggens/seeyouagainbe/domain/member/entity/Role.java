package tetoandeggens.seeyouagainbe.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String role;

    public static Role fromString(String roleStr) {
        for (Role role : Role.values()) {
            if (role.getRole().equals(roleStr)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + roleStr);
    }
}