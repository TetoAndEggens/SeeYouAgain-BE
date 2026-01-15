package tetoandeggens.seeyouagainbe.auth.oauth2.kakao.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2UnlinkServiceProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.kakao.client.KakaoApiClient;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.Map;

@Slf4j
@Service("kakaoUnlinkService")
@RequiredArgsConstructor
public class KakaoUnlinkService implements OAuth2UnlinkServiceProvider {
    private final KakaoApiClient kakaoApiClient;

    @Value("${spring.security.oauth2.client.admin.kakao.admin-key}")
    private String kakaoAdminKey;

    @Override
    public boolean unlink(Member member) {
        String socialId = member.getSocialIdKakao();
        if (socialId == null) {
            log.warn("[KakaoUnlinkService] 카카오 소셜 ID 없음");
            return true;
        }

        try {
            Map<String, Object> response = kakaoApiClient.unlinkUser(
                    "KakaoAK " + kakaoAdminKey,
                    "user_id",
                    socialId
            );
            log.info("[KakaoUnlinkService] 카카오 연동 해제 성공 - socialId: {}", socialId);
            return true;
        } catch (Exception e) {
            log.error("[KakaoUnlinkService] 카카오 연동 해제 실패 - socialId: {}", socialId, e);
            return false;
        }
    }
}
