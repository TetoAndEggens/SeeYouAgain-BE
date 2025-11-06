package tetoandeggens.seeyouagainbe.auth.oauth2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2Attributes attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return attributes.getSocialId();
    }

    public String getProvider() {
        return attributes.getProvider();
    }

    public String getSocialId() {
        return attributes.getSocialId();
    }

    public String getProfileImageUrl() {
        return attributes.getProfileImageUrl();
    }
}
