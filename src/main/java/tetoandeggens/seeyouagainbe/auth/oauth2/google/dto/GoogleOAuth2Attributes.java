package tetoandeggens.seeyouagainbe.auth.oauth2.google.dto;

import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.dto.BaseOAuth2Attributes;

import java.util.Map;

@Getter
@Builder
public class GoogleOAuth2Attributes implements BaseOAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String socialId;
    private String email;
    private String profileImageUrl;

    public static GoogleOAuth2Attributes of(String userNameAttributeName, Map<String, Object> attributes) {
        return GoogleOAuth2Attributes.builder()
                .socialId((String) attributes.get("sub"))
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("picture"))
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }

    @Override
    public String getProvider() {
        return "google";
    }
}
