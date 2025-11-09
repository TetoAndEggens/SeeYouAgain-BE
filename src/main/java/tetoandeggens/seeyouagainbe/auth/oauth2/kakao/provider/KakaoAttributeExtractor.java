package tetoandeggens.seeyouagainbe.auth.oauth2.kakao.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2AttributeExtractorProvider;

import java.util.Map;

@Slf4j
@Component("kakaoAttributeExtractor")
public class KakaoAttributeExtractor implements OAuth2AttributeExtractorProvider {

    @Override
    public String extractSocialId(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String extractProfileImageUrl(OAuth2User oAuth2User) {
        try {
            Map<String, Object> attributes = oAuth2User.getAttributes();
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    String profileImageUrl = (String) profile.get("profile_image_url");
                    if (profileImageUrl != null && !profileImageUrl.isBlank()) {
                        return profileImageUrl;
                    }
                }
            }

            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties != null) {
                return (String) properties.get("profile_image");
            }

            return null;
        } catch (Exception e) {
            log.error("[KakaoAttributeExtractor] 프로필 이미지 추출 실패", e);
            return null;
        }
    }
}
