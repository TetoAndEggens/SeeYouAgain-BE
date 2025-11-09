package tetoandeggens.seeyouagainbe.auth.oauth2.naver.dto;

import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.dto.BaseOAuth2Attributes;

import java.util.Map;

@Getter
@Builder
public class NaverOAuth2Attributes implements BaseOAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String socialId;
    private String email;
    private String profileImageUrl;

    public static NaverOAuth2Attributes of(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return NaverOAuth2Attributes.builder()
                .socialId((String) response.get("id"))
                .email((String) response.get("email"))
                .profileImageUrl((String) response.get("profile_image"))
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }

    @Override
    public String getProvider() {
        return "naver";
    }
}
