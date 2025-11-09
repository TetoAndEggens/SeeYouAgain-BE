package tetoandeggens.seeyouagainbe.auth.oauth2.kakao.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "kakao-api", url = "https://kapi.kakao.com")
public interface KakaoApiClient {

    @PostMapping("/v1/user/unlink")
    Map<String, Object> unlinkUser(
            @RequestHeader("Authorization") String adminKey,
            @RequestParam("target_id_type") String targetIdType,
            @RequestParam("target_id") String targetId
    );
}