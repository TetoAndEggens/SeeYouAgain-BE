package tetoandeggens.seeyouagainbe.auth.oauth2;

import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import java.util.Map;

@Getter
@Builder
public class OAuth2Attributes {

    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String socialId;
    private String provider;
    private String email;
    private String profileImageUrl;

    public static OAuth2Attributes of(String registrationId,
                                      String userNameAttributeName,
                                      Map<String, Object> attributes) {

        return switch (registrationId.toLowerCase()) {
            case "kakao" -> ofKakao(userNameAttributeName, attributes);
            case "naver" -> ofNaver(userNameAttributeName, attributes);
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            default -> throw new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
        };
    }

    private static OAuth2Attributes ofKakao(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2Attributes.builder()
                .socialId(String.valueOf(attributes.get("id")))
                .provider("kakao")
                .email((String) kakaoAccount.get("email"))
                .profileImageUrl((String) profile.get("profile_image_url"))
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }

    private static OAuth2Attributes ofNaver(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuth2Attributes.builder()
                .socialId((String) response.get("id"))
                .provider("naver")
                .email((String) response.get("email"))
                .profileImageUrl((String) response.get("profile_image"))
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName,
                                             Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .socialId((String) attributes.get("sub"))
                .provider("google")
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("picture"))
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }
}
