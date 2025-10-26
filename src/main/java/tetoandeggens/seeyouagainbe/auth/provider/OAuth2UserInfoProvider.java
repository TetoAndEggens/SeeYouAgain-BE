package tetoandeggens.seeyouagainbe.auth.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2UserInfoProvider {

    private final RestTemplate restTemplate;

    public String getSocialId(String provider, String accessToken) {
        log.info("[OAuth2UserInfoProvider] 소셜 ID 조회 시작 - provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "kakao" -> getKakaoSocialId(accessToken);
            case "naver" -> getNaverSocialId(accessToken);
            case "google" -> getGoogleSocialId(accessToken);
            default -> {
                log.error("[OAuth2UserInfoProvider] 지원하지 않는 플랫폼 - provider: {}", provider);
                throw new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
            }
        };
    }

    private String getKakaoSocialId(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("id")) {
                String socialId = String.valueOf(response.getBody().get("id"));
                log.info("[OAuth2UserInfoProvider] 카카오 소셜 ID 조회 성공: {}", socialId);
                return socialId;
            } else {
                log.error("[OAuth2UserInfoProvider] 카카오 응답에 id 없음");
                throw new CustomException(AuthErrorCode.OAUTH2_USER_INFO_FETCH_FAILED);
            }
        } catch (Exception e) {
            log.error("[OAuth2UserInfoProvider] 카카오 사용자 정보 조회 실패", e);
            throw new CustomException(AuthErrorCode.OAUTH2_USER_INFO_FETCH_FAILED);
        }
    }

    private String getNaverSocialId(String accessToken) {
        String userInfoUrl = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody().get("response");

                if (responseBody != null && responseBody.containsKey("id")) {
                    String socialId = String.valueOf(responseBody.get("id"));
                    log.info("[OAuth2UserInfoProvider] 네이버 소셜 ID 조회 성공: {}", socialId);
                    return socialId;
                }
            }

            log.error("[OAuth2UserInfoProvider] 네이버 응답에 id 없음");
            throw new CustomException(AuthErrorCode.OAUTH2_USER_INFO_FETCH_FAILED);
        } catch (Exception e) {
            log.error("[OAuth2UserInfoProvider] 네이버 사용자 정보 조회 실패", e);
            throw new CustomException(AuthErrorCode.OAUTH2_USER_INFO_FETCH_FAILED);
        }
    }

    private String getGoogleSocialId(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("id")) {
                String socialId = String.valueOf(response.getBody().get("id"));
                log.info("[OAuth2UserInfoProvider] 구글 소셜 ID 조회 성공: {}", socialId);
                return socialId;
            } else {
                log.error("[OAuth2UserInfoProvider] 구글 응답에 id 없음");
                throw new CustomException(AuthErrorCode.OAUTH2_USER_INFO_FETCH_FAILED);
            }
        } catch (Exception e) {
            log.error("[OAuth2UserInfoProvider] 구글 사용자 정보 조회 실패", e);
            throw new CustomException(AuthErrorCode.OAUTH2_USER_INFO_FETCH_FAILED);
        }
    }
}
