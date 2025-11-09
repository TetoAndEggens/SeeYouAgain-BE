package tetoandeggens.seeyouagainbe.auth.oauth2.common.dto;

import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2Provider;
import tetoandeggens.seeyouagainbe.auth.oauth2.google.dto.GoogleOAuth2Attributes;
import tetoandeggens.seeyouagainbe.auth.oauth2.kakao.dto.KakaoOAuth2Attributes;
import tetoandeggens.seeyouagainbe.auth.oauth2.naver.dto.NaverOAuth2Attributes;

import java.util.Map;

@Getter
@Builder
public class OAuth2Attributes {
    public static BaseOAuth2Attributes of(String registrationId,
                                          String userNameAttributeName,
                                          Map<String, Object> attributes) {
        OAuth2Provider provider = OAuth2Provider.fromRegistrationId(registrationId);

        return switch (provider) {
            case KAKAO -> KakaoOAuth2Attributes.of(userNameAttributeName, attributes);
            case NAVER -> NaverOAuth2Attributes.of(userNameAttributeName, attributes);
            case GOOGLE -> GoogleOAuth2Attributes.of(userNameAttributeName, attributes);
        };
    }
}
