package tetoandeggens.seeyouagainbe.auth.oauth2.common.provider;

import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2AttributeExtractorProvider {
    String extractSocialId(OAuth2User oAuth2User);
    String extractProfileImageUrl(OAuth2User oAuth2User);
}