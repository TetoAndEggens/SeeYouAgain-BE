package tetoandeggens.seeyouagainbe.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.oauth2.CustomOAuth2User;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            TokenProvider tokenProvider,
            MemberRepository memberRepository,
            RedisTemplate<String, String> redisTemplate,
            @Lazy OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.tokenProvider = tokenProvider;
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        log.info("[OAuth2Success] 소셜 로그인 성공 핸들러 시작");

        if (!(authentication instanceof OAuth2AuthenticationToken oAuth2Token)) {
            log.error("[OAuth2Success] 잘못된 Authentication 타입");
            redirectToError(response, "invalid_authentication");
            return;
        }

        OAuth2User oAuth2User = oAuth2Token.getPrincipal();

        // ⭐ provider 추출 (registrationId 사용)
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();

        // ⭐ socialId, profileImageUrl 추출 (OIDC와 일반 OAuth2 모두 지원)
        String socialId = extractSocialId(oAuth2User, provider);
        String profileImageUrl = extractProfileImageUrl(oAuth2User, provider);

        log.info("[OAuth2Success] 소셜 로그인 정보 - provider: {}, socialId: {}", provider, socialId);

        // OAuth2 토큰 저장 (네이버/구글만 해당)
        saveOAuth2Tokens(oAuth2Token, provider, socialId);

        // 회원 존재 여부 확인
        Optional<Member> memberOptional = findMemberBySocialId(provider, socialId);

        if (memberOptional.isPresent()) {
            // 기존 회원 → 즉시 로그인
            handleExistingMember(memberOptional.get(), provider, socialId, response);
        } else {
            // 신규 회원 → 회원가입 페이지로 리다이렉트 (JWT 발급 안 함)
            handleNewMember(provider, socialId, profileImageUrl, response);
        }
    }

    /**
     * ⭐ socialId 추출 (OIDC와 일반 OAuth2 모두 지원)
     */
    private String extractSocialId(OAuth2User oAuth2User, String provider) {
        if (oAuth2User instanceof OidcUser oidcUser) {
            // OIDC (구글)
            return oidcUser.getSubject(); // "sub" claim
        } else if (oAuth2User instanceof CustomOAuth2User customUser) {
            // CustomOAuth2User (카카오, 네이버)
            return customUser.getSocialId();
        } else {
            // 일반 OAuth2User
            Map<String, Object> attributes = oAuth2User.getAttributes();
            return switch (provider.toLowerCase()) {
                case "kakao" -> String.valueOf(attributes.get("id"));
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) response.get("id");
                }
                case "google" -> (String) attributes.get("sub");
                default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
            };
        }
    }

    /**
     * ⭐ profileImageUrl 추출 (OIDC와 일반 OAuth2 모두 지원)
     */
    private String extractProfileImageUrl(OAuth2User oAuth2User, String provider) {
        if (oAuth2User instanceof OidcUser oidcUser) {
            // OIDC (구글)
            return oidcUser.getAttribute("picture");
        } else if (oAuth2User instanceof CustomOAuth2User customUser) {
            // CustomOAuth2User (카카오, 네이버)
            return customUser.getProfileImageUrl();
        } else {
            // 일반 OAuth2User
            Map<String, Object> attributes = oAuth2User.getAttributes();
            return switch (provider.toLowerCase()) {
                case "kakao" -> {
                    Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
                    yield (String) properties.get("profile_image");
                }
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) response.get("profile_image");
                }
                case "google" -> (String) attributes.get("picture");
                default -> null;
            };
        }
    }

    /**
     * OAuth2 Access Token과 Refresh Token 저장 (네이버/구글만)
     */
    private void saveOAuth2Tokens(
            OAuth2AuthenticationToken oAuth2Token,
            String provider,
            String socialId
    ) {
        // 카카오는 Admin Key로 직접 해제 가능하므로 토큰 저장 불필요
        if (!"naver".equals(provider) && !"google".equals(provider)) {
            log.debug("[OAuth2Success] {} 는 OAuth2 토큰 저장 불필요 (Admin Key 사용)", provider);
            return;
        }

        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oAuth2Token.getAuthorizedClientRegistrationId(),
                    oAuth2Token.getName()
            );

            if (authorizedClient == null) {
                log.warn("[OAuth2Success] {} OAuth2AuthorizedClient가 null입니다", provider);
                return;
            }

            // 1. AccessToken 저장 (Redis - 연동 해제용, 30일 TTL)
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            if (accessToken != null) {
                String accessTokenValue = accessToken.getTokenValue();
                String redisKey = "oauth2:token:" + provider + ":" + socialId;

                redisTemplate.opsForValue().set(
                        redisKey,
                        accessTokenValue,
                        Duration.ofDays(30)
                );

                log.info("[OAuth2Success] {} OAuth2 AccessToken Redis 저장 완료", provider);
            }

            // 2. RefreshToken 임시 저장 (Redis - 5분 TTL)
            OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
            if (refreshToken != null) {
                String refreshTokenValue = refreshToken.getTokenValue();
                String tempKey = "oauth2:refresh:temp:" + provider + ":" + socialId;

                redisTemplate.opsForValue().set(
                        tempKey,
                        refreshTokenValue,
                        Duration.ofMinutes(5)
                );

                log.info("[OAuth2Success] {} RefreshToken 임시 저장 완료 (5분 TTL)", provider);
            } else {
                log.warn("[OAuth2Success] {} RefreshToken이 null입니다 (최초 1회만 제공될 수 있음)", provider);
            }

        } catch (Exception e) {
            log.error("[OAuth2Success] {} OAuth2 토큰 저장 실패", provider, e);
        }
    }

    /**
     * 소셜 ID로 회원 조회
     */
    private Optional<Member> findMemberBySocialId(String provider, String socialId) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> memberRepository.findBySocialIdKakaoAndIsDeletedFalse(socialId);
            case "naver" -> memberRepository.findBySocialIdNaverAndIsDeletedFalse(socialId);
            case "google" -> memberRepository.findBySocialIdGoogleAndIsDeletedFalse(socialId);
            default -> {
                log.error("[OAuth2Success] 지원하지 않는 플랫폼 - provider: {}", provider);
                yield Optional.empty();
            }
        };
    }

    /**
     * 기존 회원 처리: JWT 발급 + RefreshToken DB 저장
     */
    private void handleExistingMember(
            Member member,
            String provider,
            String socialId,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2Success] 기존 회원 로그인 처리 - memberId: {}, provider: {}", member.getId(), provider);

        // 1. OAuth2 RefreshToken을 DB에 저장 (네이버/구글만)
        saveRefreshTokenToDatabase(member, provider, socialId);

        // 2. JWT 토큰 생성
        UserTokenResponse tokens = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );

        // 3. 쿠키에 JWT 저장
        CookieUtil.setAccessTokenCookie(response, tokens.accessToken(), 3600);
        tokenProvider.setRefreshTokenCookie(response, tokens.refreshToken());

        // 4. 프론트엔드 콜백 페이지로 리다이렉트
        String redirectUrl = frontendUrl + "/auth/callback?status=login";
        log.info("[OAuth2Success] 로그인 성공 리다이렉트 - url: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
    }

    /**
     * OAuth2 RefreshToken을 Member 엔티티에 저장
     */
    private void saveRefreshTokenToDatabase(Member member, String provider, String socialId) {
        // 카카오는 RefreshToken 저장 불필요 (Admin Key로 해제)
        if ("kakao".equals(provider)) {
            return;
        }

        String tempKey = "oauth2:refresh:temp:" + provider + ":" + socialId;
        String refreshToken = redisTemplate.opsForValue().get(tempKey);

        if (refreshToken != null) {
            switch (provider.toLowerCase()) {
                case "naver" -> {
                    member.updateNaverRefreshToken(refreshToken);
                    log.info("[OAuth2Success] 네이버 RefreshToken DB 저장 완료 - memberId: {}", member.getId());
                }
                case "google" -> {
                    member.updateGoogleRefreshToken(refreshToken);
                    log.info("[OAuth2Success] 구글 RefreshToken DB 저장 완료 - memberId: {}", member.getId());
                }
            }
            memberRepository.save(member);

            // 임시 저장소에서 삭제
            redisTemplate.delete(tempKey);
            log.debug("[OAuth2Success] {} RefreshToken 임시 저장소에서 삭제 완료", provider);
        } else {
            log.warn("[OAuth2Success] {} RefreshToken 임시 저장소에 없음 - 이미 저장되었거나 제공되지 않음", provider);
        }
    }

    /**
     * 신규 회원 처리: 회원가입 페이지로 리다이렉트 (JWT 발급 안 함!)
     */
    private void handleNewMember(
            String provider,
            String socialId,
            String profileImageUrl,
            HttpServletResponse response
    ) throws IOException {
        String tempToken = UUID.randomUUID().toString();

        // Redis에 5분간 임시 저장
        redisTemplate.opsForValue().set(
                "signup:temp:" + tempToken,
                provider + ":" + socialId + ":" + profileImageUrl,
                Duration.ofMinutes(5)
        );

        String redirectUrl = String.format(
                "%s/auth/social-signup?token=%s",
                frontendUrl, tempToken
        );
        response.sendRedirect(redirectUrl);
    }

    /**
     * 에러 페이지로 리다이렉트
     */
    private void redirectToError(HttpServletResponse response, String errorMessage) throws IOException {
        String redirectUrl = String.format(
                "%s/auth/error?message=%s",
                frontendUrl,
                errorMessage
        );

        log.error("[OAuth2Success] 에러 페이지로 리다이렉트 - message: {}", errorMessage);
        response.sendRedirect(redirectUrl);
    }
}