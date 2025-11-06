package tetoandeggens.seeyouagainbe.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "naver-api", url = "https://nid.naver.com")
public interface NaverApiClient {

    @PostMapping("/oauth2.0/token")
    Map<String, Object> getAccessToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code,
            @RequestParam("state") String state
    );

    @PostMapping("/oauth2.0/token")
    Map<String, Object> revokeToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("access_token") String accessToken,
            @RequestParam("service_provider") String serviceProvider
    );

    @PostMapping("/oauth2.0/token")
    Map<String, Object> refreshAccessToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("refresh_token") String refreshToken
    );
}