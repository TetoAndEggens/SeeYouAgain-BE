package tetoandeggens.seeyouagainbe.auth.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
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
public class OAuth2UnlinkProvider {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.security.oauth2.client.admin.kakao.admin-key}")
    private String kakaoAdminKey;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    public boolean unlinkSocialAccount(String provider, String socialId, String accessToken) {
        log.info("[OAuth2UnlinkProvider] 소셜 연동 해제 시작 - provider: {}, socialId: {}", provider, socialId);

        return switch (provider.toLowerCase()) {
            case "kakao" -> unlinkKakao(socialId);
            case "naver" -> unlinkNaver(socialId);
            case "google" -> unlinkGoogle(socialId);
            default -> {
                log.error("[OAuth2UnlinkProvider] 지원하지 않는 플랫폼 - provider: {}", provider);
                throw new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
            }
        };
    }

    private boolean unlinkKakao(String socialId) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + kakaoAdminKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", socialId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("[OAuth2UnlinkProvider] 카카오 연동 해제 성공 - socialId: {}", socialId);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("[OAuth2UnlinkProvider] 카카오 연동 해제 실패 - socialId: {}, error: {}", socialId, e.getMessage());
            return true;
        }
    }

    private boolean unlinkNaver(String socialId) {
        String accessToken = redisTemplate.opsForValue().get("naver:token:" + socialId);

        if (accessToken == null) {
            log.warn("[OAuth2UnlinkProvider] 네이버 액세스 토큰 없음 - DB에서만 제거");
            return true;
        }

        String url = "https://nid.naver.com/oauth2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "delete");
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("access_token", accessToken);
        body.add("service_provider", "NAVER");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("[OAuth2UnlinkProvider] 네이버 연동 해제 성공");
                // Redis에서 토큰 삭제
                redisTemplate.delete("naver:token:" + socialId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("[OAuth2UnlinkProvider] 네이버 연동 해제 실패 - error: {}", e.getMessage());
            return true;
        }
    }

    private boolean unlinkGoogle(String socialId) {
        String accessToken = redisTemplate.opsForValue().get("google:token:" + socialId);

        if (accessToken == null) {
            log.warn("[OAuth2UnlinkProvider] 구글 액세스 토큰 없음 - DB에서만 제거");
            return true;
        }

        String url = "https://oauth2.googleapis.com/revoke";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("[OAuth2UnlinkProvider] 구글 연동 해제 성공");
                redisTemplate.delete("google:token:" + socialId);
                return true;
            }
            log.warn("[OAuth2UnlinkProvider] 구글 연동 해제 실패 - 상태 코드: {}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("[OAuth2UnlinkProvider] 구글 연동 해제 실패 - error: {}", e.getMessage());
            return true;
        }
    }
}
