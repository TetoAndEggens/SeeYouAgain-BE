package tetoandeggens.seeyouagainbe.auth.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2TokenProvider {
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    public String getAccessToken(String provider, String code) {
        log.info("[OAuth2TokenProvider] Access Token 요청 시작 - provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "kakao" -> getKakaoAccessToken(code);
            case "naver" -> getNaverAccessToken(code);
            case "google" -> getGoogleAccessToken(code);
            default -> {
                log.error("[OAuth2TokenProvider] 지원하지 않는 플랫폼 - provider: {}", provider);
                throw new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
            }
        };
    }

    private String getKakaoAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        body.add("code", code);
        body.add("redirect_uri", kakaoRedirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                String accessToken = (String) response.getBody().get("access_token");
                log.info("[OAuth2TokenProvider] 카카오 토큰 교환 성공");
                return accessToken;
            } else {
                log.error("[OAuth2TokenProvider] 카카오 응답에 access_token 없음");
                throw new CustomException(AuthErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
            }
        } catch (Exception e) {
            log.error("[OAuth2TokenProvider] 카카오 토큰 교환 실패", e);
            throw new CustomException(AuthErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
        }
    }

    private String getNaverAccessToken(String code) {
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("code", code);
        body.add("state", "");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                String accessToken = (String) response.getBody().get("access_token");
                log.info("[OAuth2TokenProvider] 네이버 토큰 교환 성공");
                return accessToken;
            } else {
                log.error("[OAuth2TokenProvider] 네이버 응답에 access_token 없음");
                throw new CustomException(AuthErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
            }
        } catch (Exception e) {
            log.error("[OAuth2TokenProvider] 네이버 토큰 교환 실패", e);
            throw new CustomException(AuthErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
        }
    }

    private String getGoogleAccessToken(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("code", code);
        body.add("redirect_uri", googleRedirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                String accessToken = (String) response.getBody().get("access_token");
                log.info("[OAuth2TokenProvider] 구글 토큰 교환 성공");
                return accessToken;
            } else {
                log.error("[OAuth2TokenProvider] 구글 응답에 access_token 없음");
                throw new CustomException(AuthErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
            }
        } catch (Exception e) {
            log.error("[OAuth2TokenProvider] 구글 토큰 교환 실패", e);
            throw new CustomException(AuthErrorCode.OAUTH2_TOKEN_EXCHANGE_FAILED);
        }
    }
}
