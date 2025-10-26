package tetoandeggens.seeyouagainbe.auth.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginHandler {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void handleSocialLoginCallback(
            String provider,
            String socialId,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2LoginHandler] 로그인 처리 시작 - provider: {}, socialId: {}", provider, socialId);

        try {
            Optional<Member> member = findMemberBySocialId(provider, socialId);

            if (member.isPresent()) {
                Member foundMember = member.get();
                handleLoginSuccess(foundMember, response);
            } else {
                handleSignupRequired(provider, socialId, response);
            }
        } catch (Exception e) {
            log.error("[OAuth2LoginHandler] 로그인 처리 실패", e);
            redirectToError(response, "social_login_failed");
        }
    }

    private void handleLoginSuccess(
            Member member,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2LoginHandler] 로그인 성공 처리 - memberId: {}", member.getId());

        UserTokenResponse tokens = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );

        tokenProvider.setRefreshTokenCookie(response, tokens.refreshToken());

        String redirectUrl = String.format(
                "%s/auth/callback?accessToken=%s",
                frontendUrl,
                tokens.accessToken()
        );

        log.info("[OAuth2LoginHandler] 프론트엔드로 리다이렉트 - url: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void handleSignupRequired(
            String provider,
            String socialId,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2LoginHandler] 신규 회원가입 필요 - provider: {}", provider);

        String redirectUrl = String.format(
                "%s/auth/social-signup?provider=%s&socialId=%s",
                frontendUrl,
                provider,
                socialId
        );

        log.info("[OAuth2LoginHandler] 회원가입 페이지로 리다이렉트 - url: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void redirectToError(
            HttpServletResponse response,
            String errorMessage
    ) throws IOException {
        String redirectUrl = String.format(
                "%s/auth/error?message=%s",
                frontendUrl,
                errorMessage
        );

        log.info("[OAuth2LoginHandler] 오류 페이지로 리다이렉트 - url: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private Optional<Member> findMemberBySocialId(String provider, String socialId) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> memberRepository.findBySocialIdKakaoAndIsDeletedFalse(socialId);
            case "naver" -> memberRepository.findBySocialIdNaverAndIsDeletedFalse(socialId);
            case "google" -> memberRepository.findBySocialIdGoogleAndIsDeletedFalse(socialId);
            default -> {
                log.error("[OAuth2LoginHandler] 지원하지 않는 플랫폼 - provider: {}", provider);
                throw new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
            }
        };
    }
}
