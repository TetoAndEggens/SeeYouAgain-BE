package tetoandeggens.seeyouagainbe.auth.oauth2.google.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2UnlinkServiceProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.google.client.GoogleApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("googleUnlinkService")
@RequiredArgsConstructor
public class GoogleUnlinkService implements OAuth2UnlinkServiceProvider {

    private final GoogleApiClient googleApiClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Override
    public boolean unlink(Member member) {
        String refreshToken = member.getGoogleRefreshToken();
        if (refreshToken == null) {
            log.warn("[GoogleUnlinkService] RefreshToken 없음 - DB에서만 제거");
            return true;
        }

        try {
            // 1. RefreshToken으로 AccessToken 재발급
            Map<String, Object> refreshParams = new HashMap<>();
            refreshParams.put("grant_type", "refresh_token");
            refreshParams.put("client_id", googleClientId);
            refreshParams.put("client_secret", googleClientSecret);
            refreshParams.put("refresh_token", refreshToken);

            Map<String, Object> tokenResponse = googleApiClient.refreshAccessToken(refreshParams);

            String accessToken = (String) tokenResponse.get("access_token");
            if (accessToken == null) {
                log.error("[GoogleUnlinkService] AccessToken 재발급 실패: {}", tokenResponse);
                return false;
            }

            // 2. AccessToken으로 연동 해제
            Map<String, Object> revokeParams = new HashMap<>();
            revokeParams.put("token", accessToken);

            googleApiClient.revokeToken(revokeParams);
            return true;

        } catch (Exception e) {
            log.error("[GoogleUnlinkService] 구글 연동 해제 실패", e);
            return false;
        }
    }
}
