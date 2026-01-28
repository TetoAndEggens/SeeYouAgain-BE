package tetoandeggens.seeyouagainbe.auth.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2Provider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2AttributeExtractorProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.service.OAuth2TokenExtractor;
import tetoandeggens.seeyouagainbe.auth.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import tetoandeggens.seeyouagainbe.auth.service.CookieService;
import tetoandeggens.seeyouagainbe.auth.service.RedisAuthService;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.REDIRECT_URI_PARAM_COOKIE_NAME;
import static tetoandeggens.seeyouagainbe.global.constants.AuthVerificationConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final Map<String, OAuth2AttributeExtractorProvider> attributeExtractors;
    private final OAuth2TokenExtractor tokenExtractor;
    private final RedisAuthService redisAuthService;
    private final CookieService cookieService;
    private final HttpCookieOAuth2AuthorizationRequestRepository authRequestRepository;
    private final CookieUtil cookieUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final List<String> ALLOWED_LOCAL_ORIGINS = List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    );

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        authRequestRepository.removeAuthorizationRequestCookies(request, response);

        if (!(authentication instanceof OAuth2AuthenticationToken oAuth2Token)) {
            log.error("[OAuth2Success] 잘못된 Authentication 타입");
            return;
        }

        OAuth2User oAuth2User = oAuth2Token.getPrincipal();
        String registrationId = oAuth2Token.getAuthorizedClientRegistrationId();
        OAuth2Provider provider = OAuth2Provider.fromRegistrationId(registrationId);

        String extractorBeanName = provider.getRegistrationId() + "AttributeExtractor";
        OAuth2AttributeExtractorProvider extractor = attributeExtractors.get(extractorBeanName);

        if (extractor == null) {
            log.error("[OAuth2Success] AttributeExtractor를 찾을 수 없음 - provider: {}", provider.getRegistrationId());
            return;
        }

        String socialId = extractor.extractSocialId(oAuth2User);
        String profileImageUrl = extractor.extractProfileImageUrl(oAuth2User);
        String oauthRefreshToken = tokenExtractor.extractRefreshToken(oAuth2Token, provider);

        Optional<Member> memberOptional = findMemberBySocialId(provider, socialId);

        if (memberOptional.isPresent()) {
            handleExistingMember(request, memberOptional.get(), response);
        } else {
            handleNewMember(request, provider, socialId, profileImageUrl, oauthRefreshToken, response);
        }
    }

    private Optional<Member> findMemberBySocialId(OAuth2Provider provider, String socialId) {
        return switch (provider) {
            case KAKAO -> memberRepository.findBySocialIdKakaoAndIsDeletedFalse(socialId);
            case NAVER -> memberRepository.findBySocialIdNaverAndIsDeletedFalse(socialId);
            case GOOGLE -> memberRepository.findBySocialIdGoogleAndIsDeletedFalse(socialId);
        };
    }

    private void handleExistingMember(HttpServletRequest request, Member member, HttpServletResponse response) throws IOException {
        UserTokenResponse tokens = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );

        cookieService.setAccessTokenCookie(
                response,
                tokens.accessToken(),
                tokenProvider.getAccessTokenExpirationSec()
        );
        cookieService.setRefreshTokenCookie(
                response,
                tokens.refreshToken(),
                tokenProvider.getRefreshTokenExpirationSec()
        );

        redisAuthService.saveRefreshToken(
                member.getUuid(),
                tokens.refreshToken(),
                tokenProvider.getRefreshTokenExpirationMs()
        );

        redisAuthService.saveMemberId(
                member.getUuid(),
                member.getId(),
                tokenProvider.getRefreshTokenExpirationMs()
        );

        String targetFrontendUrl = determineTargetUrl(request);

        String redirectUrl = UriComponentsBuilder.fromUriString(targetFrontendUrl)
                .path("/auth/callback")
                .queryParam("status", "login")
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private void handleNewMember(
            HttpServletRequest request,
            OAuth2Provider provider,
            String socialId,
            String profileImageUrl,
            String oauthRefreshToken,
            HttpServletResponse response
    ) throws IOException {
        String tempUuid = UUID.randomUUID().toString();

        redisAuthService.saveTempSocialInfo(
                tempUuid,
                provider.getRegistrationId(),
                socialId,
                oauthRefreshToken
        );

        String tempToken = tokenProvider.createSocialTempToken(
                provider.getRegistrationId(),
                profileImageUrl,
                tempUuid
        );

        cookieService.setSocialTempTokenCookie(response, tempToken, VERIFICATION_TIME * 60L);

        String targetFrontendUrl = determineTargetUrl(request);

        String redirectUrl = UriComponentsBuilder.fromUriString(targetFrontendUrl)
                .path("/auth/social-signup")
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String determineTargetUrl(HttpServletRequest request) {
        String redirectUriFromCookie = cookieUtil.resolveCookieValue(request, REDIRECT_URI_PARAM_COOKIE_NAME);

        if (redirectUriFromCookie != null && ALLOWED_LOCAL_ORIGINS.contains(redirectUriFromCookie)) {
            return redirectUriFromCookie;
        }

        return frontendUrl;
    }
}