package tetoandeggens.seeyouagainbe.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialAccountLinkStrategy {

    public void linkSocialId(Member member, String provider, String socialId, String refreshToken) {
        switch (provider.toLowerCase()) {
            case "kakao" -> linkKakao(member, socialId);
            case "naver" -> linkNaver(member, socialId, refreshToken);
            case "google" -> linkGoogle(member, socialId, refreshToken);
            default -> throw new IllegalArgumentException("지원하지 않는 Provider: " + provider);
        }
    }

    private void linkKakao(Member member, String socialId) {
        member.updateKakaoSocialId(socialId);
    }

    private void linkNaver(Member member, String socialId, String refreshToken) {
        member.updateNaverSocialId(socialId);
        if (refreshToken != null && !refreshToken.isBlank()) {
            member.updateNaverRefreshToken(refreshToken);
        }
    }

    private void linkGoogle(Member member, String socialId, String refreshToken) {
        member.updateGoogleSocialId(socialId);
        if (refreshToken != null && !refreshToken.isBlank()) {
            member.updateGoogleRefreshToken(refreshToken);
        }
    }
}