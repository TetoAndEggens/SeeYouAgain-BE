package tetoandeggens.seeyouagainbe.auth.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static tetoandeggens.seeyouagainbe.global.constants.AuthConstants.SOCIAL_TEMP_TOKEN;
import static tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final Map<String, OAuth2AttributeExtractorProvider> attributeExtractors;
    private final OAuth2TokenExtractor tokenExtractor;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oAuth2Token)) {
            log.error("[OAuth2Success] 잘못된 Authentication 타입");
            return;
        }

        OAuth2User oAuth2User = oAuth2Token.getPrincipal();
        String registrationId = oAuth2Token.getAuthorizedClientRegistrationId();

        OAuth2Provider provider = OAuth2Provider.fromRegistrationId(registrationId);

        // Provider별 Extractor 가져오기
        String extractorBeanName = provider.getRegistrationId() + "AttributeExtractor";
        OAuth2AttributeExtractorProvider extractor = attributeExtractors.get(extractorBeanName);

        if (extractor == null) {
            log.error("[OAuth2Success] AttributeExtractor를 찾을 수 없음 - provider: {}", provider.getRegistrationId());
            return;
        }

        // socialId, profileImageUrl 추출
        String socialId = extractor.extractSocialId(oAuth2User);
        String profileImageUrl = extractor.extractProfileImageUrl(oAuth2User);
        String oauthRefreshToken = tokenExtractor.extractRefreshToken(oAuth2Token, provider);

        // 회원 존재 여부 확인
        Optional<Member> memberOptional = findMemberBySocialId(provider, socialId);

        if (memberOptional.isPresent()) {
            handleExistingMember(memberOptional.get(), provider, response);
        } else {
            handleNewMember(provider, socialId, profileImageUrl, oauthRefreshToken, response);
        }
    }

    private Optional<Member> findMemberBySocialId(OAuth2Provider provider, String socialId) {
        return switch (provider) {
            case KAKAO -> memberRepository.findBySocialIdKakaoAndIsDeletedFalse(socialId);
            case NAVER -> memberRepository.findBySocialIdNaverAndIsDeletedFalse(socialId);
            case GOOGLE -> memberRepository.findBySocialIdGoogleAndIsDeletedFalse(socialId);
        };
    }

    private void handleExistingMember(
            Member member,
            OAuth2Provider provider,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2Success] 기존 회원 로그인 - memberId: {}, provider: {}",
                member.getId(), provider.getRegistrationId());
        // JWT 토큰 생성
        UserTokenResponse tokens = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );

        tokenProvider.setAccessTokenCookie(response, tokens.accessToken());
        tokenProvider.setRefreshTokenCookie(response, tokens.refreshToken());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("status", "login")
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private void handleNewMember(
            OAuth2Provider provider,
            String socialId,
            String profileImageUrl,
            String oauthRefreshToken,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2Success] 신규 회원 - provider: {}, socialId: {}", provider.getRegistrationId(), socialId);

        String tempUuid = UUID.randomUUID().toString();
        saveSocialInfoToRedis(tempUuid, provider.getRegistrationId(), socialId, oauthRefreshToken);

        // RefreshToken 포함한 임시 JWT 생성
        String tempToken = tokenProvider.createSocialTempToken(
                provider.getRegistrationId(),
                profileImageUrl,
                tempUuid
        );

        CookieUtil.setCookie(response, SOCIAL_TEMP_TOKEN, tempToken, VERIFICATION_TIME * 60L);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/social-signup")
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private void saveSocialInfoToRedis(String tempUuid, String provider, String socialId, String refreshToken) {
        Duration ttl = Duration.ofMinutes(VERIFICATION_TIME);

        redisTemplate.opsForValue().set(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid, provider, ttl);
        redisTemplate.opsForValue().set(PREFIX_TEMP_SOCIAL_ID + tempUuid, socialId, ttl);

        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.opsForValue().set(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid, refreshToken, ttl);
            log.info("[OAuth2Success] {} RefreshToken Redis 저장 완료 - tempUuid: {}", provider, tempUuid);
        }

        log.info("[OAuth2Success] 소셜 정보 Redis 저장 완료 - tempUuid: {}, TTL: {}분", tempUuid, VERIFICATION_TIME);
    }
}