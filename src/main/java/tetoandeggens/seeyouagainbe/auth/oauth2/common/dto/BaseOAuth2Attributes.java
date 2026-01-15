package tetoandeggens.seeyouagainbe.auth.oauth2.common.dto;

import java.util.Map;

public interface BaseOAuth2Attributes {
    Map<String, Object> getAttributes();
    String getSocialId();
    String getProfileImageUrl();
    String getProvider();
}
