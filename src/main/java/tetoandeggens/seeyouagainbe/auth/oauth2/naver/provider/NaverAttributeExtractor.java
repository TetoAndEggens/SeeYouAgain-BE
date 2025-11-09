package tetoandeggens.seeyouagainbe.auth.oauth2.naver.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2AttributeExtractorProvider;

import java.util.Map;

@Slf4j
@Component("naverAttributeExtractor")
public class NaverAttributeExtractor implements OAuth2AttributeExtractorProvider {

    @Override
    public String extractSocialId(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return (String) response.get("id");
    }

    @Override
    public String extractProfileImageUrl(OAuth2User oAuth2User) {
        try {
            Map<String, Object> attributes = oAuth2User.getAttributes();
            Object responseObj = attributes.get("response");

            if (responseObj instanceof Map<?, ?> response) {
                String profileImage = (String) response.get("profile_image");
                if (profileImage != null && !profileImage.isBlank()) {
                    return profileImage;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("[NaverAttributeExtractor] 프로필 이미지 추출 실패", e);
            return null;
        }
    }
}
