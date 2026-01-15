package tetoandeggens.seeyouagainbe.auth.oauth2.naver.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2UnlinkServiceProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.naver.client.NaverApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.Map;

@Slf4j
@Service("naverUnlinkService")
@RequiredArgsConstructor
public class NaverUnlinkService implements OAuth2UnlinkServiceProvider {

    private final NaverApiClient naverApiClient;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Override
    public boolean unlink(Member member) {
        String refreshToken = member.getNaverRefreshToken();
        if (refreshToken == null) {
            log.warn("[NaverUnlinkService] RefreshToken 없음 - DB에서만 제거");
            return true;
        }

        try {
            // 1. RefreshToken으로 AccessToken 재발급
            Map<String, Object> tokenResponse = naverApiClient.refreshAccessToken(
                    "refresh_token",
                    naverClientId,
                    naverClientSecret,
                    refreshToken
            );

            String accessToken = (String) tokenResponse.get("access_token");

            // 2. AccessToken으로 연동 해제
            naverApiClient.revokeToken(
                    "delete",
                    naverClientId,
                    naverClientSecret,
                    accessToken,
                    "NAVER"
            );
            log.info("[NaverUnlinkService] 네이버 연동 해제 성공");
            return true;

        } catch (Exception e) {
            log.error("[NaverUnlinkService] 네이버 연동 해제 실패", e);
            return false;
        }
    }
}
