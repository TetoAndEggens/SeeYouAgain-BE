package tetoandeggens.seeyouagainbe.auth.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tetoandeggens.seeyouagainbe.auth.client.GoogleApiClient;
import tetoandeggens.seeyouagainbe.auth.client.KakaoApiClient;
import tetoandeggens.seeyouagainbe.auth.client.NaverApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2UnlinkProvider {

    private final KakaoApiClient kakaoApiClient;
    private final NaverApiClient naverApiClient;
    private final GoogleApiClient googleApiClient;

    @Value("${spring.security.oauth2.client.admin.kakao.admin-key}")
    private String kakaoAdminKey;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    public boolean unlinkKakao(String socialId) {
        if (socialId == null) {
            log.warn("[OAuth2UnlinkProvider] 카카오 소셜 ID 없음");
            return true;
        }

        try {
            Map<String, Object> response = kakaoApiClient.unlinkUser(
                    "KakaoAK " + kakaoAdminKey,
                    "user_id",
                    socialId
            );
            log.info("[OAuth2UnlinkProvider] 카카오 연동 해제 성공 - socialId: {}", socialId);
            return true;
        } catch (Exception e) {
            log.error("[OAuth2UnlinkProvider] 카카오 연동 해제 실패 - socialId: {}, error: {}",
                    socialId, e.getMessage());
            return true; // 실패해도 DB에서는 제거
        }
    }

    public boolean unlinkNaver(Member member) {
        String refreshToken = member.getNaverRefreshToken();
        if (refreshToken == null) {
            log.warn("[OAuth2UnlinkProvider] 네이버 RefreshToken 없음 - DB에서만 제거");
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

            log.info("[OAuth2UnlinkProvider] 네이버 연동 해제 성공");
            return true;

        } catch (Exception e) {
            log.error("[OAuth2UnlinkProvider] 네이버 연동 해제 실패", e);
            return true; // 실패해도 DB에서는 제거
        }
    }

    public boolean unlinkGoogle(Member member) {
        String refreshToken = member.getGoogleRefreshToken();
        if (refreshToken == null) {
            log.warn("[OAuth2UnlinkProvider] 구글 RefreshToken 없음 - DB에서만 제거");
            return true;
        }

        try {
            // 1. RefreshToken으로 AccessToken 재발급
            MultiValueMap<String, String> refreshParams = new LinkedMultiValueMap<>();
            refreshParams.add("grant_type", "refresh_token");
            refreshParams.add("client_id", googleClientId);
            refreshParams.add("client_secret", googleClientSecret);
            refreshParams.add("refresh_token", refreshToken);

            Map<String, Object> tokenResponse = googleApiClient.refreshAccessToken(refreshParams);
            String accessToken = (String) tokenResponse.get("access_token");

            // 2. AccessToken으로 연동 해제
            MultiValueMap<String, String> revokeParams = new LinkedMultiValueMap<>();
            revokeParams.add("token", accessToken);

            googleApiClient.revokeToken(revokeParams);

            log.info("[OAuth2UnlinkProvider] 구글 연동 해제 성공");
            return true;

        } catch (Exception e) {
            log.error("[OAuth2UnlinkProvider] 구글 연동 해제 실패", e);
            return true; // 실패해도 DB에서는 제거
        }
    }
}