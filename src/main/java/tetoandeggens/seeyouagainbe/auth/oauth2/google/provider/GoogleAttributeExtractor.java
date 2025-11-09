package tetoandeggens.seeyouagainbe.auth.oauth2.google.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2AttributeExtractorProvider;

import java.util.Map;

@Slf4j
@Component("googleAttributeExtractor")
public class GoogleAttributeExtractor implements OAuth2AttributeExtractorProvider {

    @Override
    public String extractSocialId(OAuth2User oAuth2User) {
        if (oAuth2User instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        return (String) attributes.get("sub");
    }

    @Override
    public String extractProfileImageUrl(OAuth2User oAuth2User) {
        if (oAuth2User instanceof OidcUser oidcUser) {
            return oidcUser.getAttribute("picture");
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        return (String) attributes.get("picture");
    }
}
