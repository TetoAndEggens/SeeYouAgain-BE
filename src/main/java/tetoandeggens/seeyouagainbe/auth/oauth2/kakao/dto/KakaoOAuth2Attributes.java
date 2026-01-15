package tetoandeggens.seeyouagainbe.auth.oauth2.kakao.dto;

import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.dto.BaseOAuth2Attributes;

import java.util.Map;

@Getter
@Builder
public class KakaoOAuth2Attributes implements BaseOAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String socialId;
    private String email;
    private String profileImageUrl;

    public static KakaoOAuth2Attributes of(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return KakaoOAuth2Attributes.builder()
                .socialId(String.valueOf(attributes.get("id")))
                .email((String) kakaoAccount.get("email"))
                .profileImageUrl((String) profile.get("profile_image_url"))
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }

    @Override
    public String getProvider() {
        return "kakao";
    }
}
