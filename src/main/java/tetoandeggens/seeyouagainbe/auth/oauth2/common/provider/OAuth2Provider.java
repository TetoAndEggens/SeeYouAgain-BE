package tetoandeggens.seeyouagainbe.auth.oauth2.common.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum OAuth2Provider {
    KAKAO("kakao", "id", "kakao_account.profile.profile_image_url", false, "https://kapi.kakao.com/v1/user/unlink"),
    NAVER("naver", "response.id", "response.profile_image", true, "https://nid.naver.com/oauth2.0/token"),
    GOOGLE("google", "sub", "picture", true, "https://oauth2.googleapis.com/revoke");

    private final String registrationId;
    private final String socialIdPath;
    private final String profileImagePath;
    private final boolean requiresRefreshToken;
    private final String unlinkUrl;

    public static OAuth2Provider fromRegistrationId(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.registrationId.equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH2_PROVIDER));
    }

    public boolean isKakao() {
        return this == KAKAO;
    }

    public boolean isNaver() {
        return this == NAVER;
    }

    public boolean isGoogle() {
        return this == GOOGLE;
    }
}
