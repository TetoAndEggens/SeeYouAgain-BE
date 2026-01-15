package tetoandeggens.seeyouagainbe.auth.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2AttributeExtractorProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2Provider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.service.OAuth2TokenExtractor;
import tetoandeggens.seeyouagainbe.auth.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import tetoandeggens.seeyouagainbe.auth.service.CookieService;
import tetoandeggens.seeyouagainbe.auth.service.RedisAuthService;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.entity.Role;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static tetoandeggens.seeyouagainbe.global.constants.AuthVerificationConstants.VERIFICATION_TIME;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationSuccessHandler 단위 테스트")
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock private TokenProvider tokenProvider;
    @Mock private MemberRepository memberRepository;
    @Mock private Map<String, OAuth2AttributeExtractorProvider> attributeExtractors;
    @Mock private OAuth2TokenExtractor tokenExtractor;
    @Mock private RedisAuthService redisAuthService;
    @Mock private CookieService cookieService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private OAuth2AuthenticationToken oAuth2Token;
    @Mock private OAuth2User oAuth2User;
    @Mock private OAuth2AttributeExtractorProvider attributeExtractor;
    @Mock private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    private static final String TEST_SOCIAL_ID = "kakao123456";
    private static final String TEST_PROFILE_URL = "https://example.com/profile.jpg";
    private static final String TEST_REFRESH_TOKEN = "oauth-refresh-token";
    private static final String TEST_UUID = "test-uuid-123";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final String TEST_JWT_REFRESH_TOKEN = "test-jwt-refresh-token";
    private static final String FRONTEND_URL = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", FRONTEND_URL);
    }

    @Nested
    @DisplayName("기존 회원 로그인 테스트")
    class ExistingMemberTests {

        @Nested
        @DisplayName("HttpCookieOAuth2AuthorizationRequestRepository 연동 테스트")
        class HttpCookieRepositoryIntegrationTests {

            @Test
            @DisplayName("OAuth2 성공 시 AuthorizationRequest 쿠키가 삭제된다")
            void shouldRemoveAuthorizationRequestCookies_OnSuccess() throws IOException {
                setupOAuth2Authentication("kakao");

                // given
                Member existingMember = Member.builder()
                        .loginId("testuser")
                        .password("password")
                        .nickName("테스트")
                        .phoneNumber("01012345678")
                        .build();
                ReflectionTestUtils.setField(existingMember, "role", Role.USER);
                ReflectionTestUtils.setField(existingMember, "uuid", TEST_UUID);
                ReflectionTestUtils.setField(existingMember, "socialIdKakao", TEST_SOCIAL_ID);

                when(memberRepository.findBySocialIdKakaoAndIsDeletedFalse(TEST_SOCIAL_ID))
                        .thenReturn(Optional.of(existingMember));
                when(tokenProvider.createLoginToken(TEST_UUID, Role.USER))
                        .thenReturn(new UserTokenResponse(TEST_ACCESS_TOKEN, TEST_JWT_REFRESH_TOKEN));
                doNothing().when(httpCookieOAuth2AuthorizationRequestRepository)
                        .removeAuthorizationRequestCookies(any(HttpServletRequest.class), any(HttpServletResponse.class));

                // when
                handler.onAuthenticationSuccess(request, response, oAuth2Token);

                // then
                verify(httpCookieOAuth2AuthorizationRequestRepository, times(1))
                        .removeAuthorizationRequestCookies(request, response);
            }
        }

        @Test
        @DisplayName("기존 카카오 회원 로그인 - 성공")
        void existingKakaoMember_LoginSuccess() throws IOException {
            setupOAuth2Authentication("kakao");

            Member existingMember = Member.builder()
                    .loginId("testuser")
                    .password("password")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();
            ReflectionTestUtils.setField(existingMember, "role", Role.USER);
            ReflectionTestUtils.setField(existingMember, "uuid", TEST_UUID);
            ReflectionTestUtils.setField(existingMember, "socialIdKakao", TEST_SOCIAL_ID);

            UserTokenResponse tokenResponse = new UserTokenResponse(TEST_ACCESS_TOKEN, TEST_JWT_REFRESH_TOKEN);

            when(memberRepository.findBySocialIdKakaoAndIsDeletedFalse(TEST_SOCIAL_ID))
                    .thenReturn(Optional.of(existingMember));
            when(tokenProvider.createLoginToken(TEST_UUID, Role.USER)).thenReturn(tokenResponse);
            when(tokenProvider.getAccessTokenExpirationSec()).thenReturn(3600L);
            when(tokenProvider.getRefreshTokenExpirationSec()).thenReturn(86400L);
            when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(86400000L);
            doNothing().when(cookieService).setAccessTokenCookie(any(), anyString(), anyLong());
            doNothing().when(cookieService).setRefreshTokenCookie(any(), anyString(), anyLong());
            doNothing().when(redisAuthService).saveRefreshToken(anyString(), anyString(), anyLong());
            doNothing().when(response).sendRedirect(anyString());

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(memberRepository).findBySocialIdKakaoAndIsDeletedFalse(TEST_SOCIAL_ID);
            verify(tokenProvider).createLoginToken(TEST_UUID, Role.USER);
            verify(cookieService).setAccessTokenCookie(response, TEST_ACCESS_TOKEN, 3600L);
            verify(cookieService).setRefreshTokenCookie(response, TEST_JWT_REFRESH_TOKEN, 86400L);
            verify(redisAuthService).saveRefreshToken(TEST_UUID, TEST_JWT_REFRESH_TOKEN, 86400000L);
            verify(response).sendRedirect(contains(FRONTEND_URL));
        }

        @Test
        @DisplayName("기존 네이버 회원 로그인 - 성공")
        void existingNaverMember_LoginSuccess() throws IOException {
            setupOAuth2Authentication("naver");

            Member existingMember = Member.builder()
                    .loginId("testuser")
                    .password("password")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();
            ReflectionTestUtils.setField(existingMember, "role", Role.USER);
            ReflectionTestUtils.setField(existingMember, "uuid", TEST_UUID);
            ReflectionTestUtils.setField(existingMember, "socialIdNaver", TEST_SOCIAL_ID);

            UserTokenResponse tokenResponse = new UserTokenResponse(TEST_ACCESS_TOKEN, TEST_JWT_REFRESH_TOKEN);

            when(memberRepository.findBySocialIdNaverAndIsDeletedFalse(TEST_SOCIAL_ID))
                    .thenReturn(Optional.of(existingMember));
            when(tokenProvider.createLoginToken(TEST_UUID, Role.USER)).thenReturn(tokenResponse);

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(memberRepository).findBySocialIdNaverAndIsDeletedFalse(TEST_SOCIAL_ID);
            verify(tokenProvider).createLoginToken(TEST_UUID, Role.USER);
        }

        @Test
        @DisplayName("기존 구글 회원 로그인 - 성공")
        void existingGoogleMember_LoginSuccess() throws IOException {
            setupOAuth2Authentication("google");

            Member existingMember = Member.builder()
                    .loginId("testuser")
                    .password("password")
                    .nickName("테스트")
                    .phoneNumber("01012345678")
                    .build();
            ReflectionTestUtils.setField(existingMember, "role", Role.USER);
            ReflectionTestUtils.setField(existingMember, "uuid", TEST_UUID);
            ReflectionTestUtils.setField(existingMember, "socialIdGoogle", TEST_SOCIAL_ID);

            UserTokenResponse tokenResponse = new UserTokenResponse(TEST_ACCESS_TOKEN, TEST_JWT_REFRESH_TOKEN);

            when(memberRepository.findBySocialIdGoogleAndIsDeletedFalse(TEST_SOCIAL_ID))
                    .thenReturn(Optional.of(existingMember));
            when(tokenProvider.createLoginToken(TEST_UUID, Role.USER)).thenReturn(tokenResponse);

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(memberRepository).findBySocialIdGoogleAndIsDeletedFalse(TEST_SOCIAL_ID);
            verify(tokenProvider).createLoginToken(TEST_UUID, Role.USER);
        }
    }

    @Nested
    @DisplayName("신규 회원 처리 테스트")
    class NewMemberTests {

        @Test
        @DisplayName("신규 카카오 회원 - 임시 정보 저장 및 리다이렉트")
        void newKakaoMember_SavesTempInfoAndRedirects() throws IOException {
            setupOAuth2Authentication("kakao");

            when(memberRepository.findBySocialIdKakaoAndIsDeletedFalse(TEST_SOCIAL_ID))
                    .thenReturn(Optional.empty());
            when(tokenProvider.createSocialTempToken(anyString(), anyString(), anyString()))
                    .thenReturn("temp-token-123");
            doNothing().when(redisAuthService).saveTempSocialInfo(anyString(), anyString(), anyString(), anyString());
            doNothing().when(cookieService).setSocialTempTokenCookie(any(), anyString(), anyLong());
            doNothing().when(response).sendRedirect(anyString());

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(redisAuthService).saveTempSocialInfo(anyString(), eq("kakao"), eq(TEST_SOCIAL_ID), eq(TEST_REFRESH_TOKEN));
            verify(cookieService).setSocialTempTokenCookie(response, "temp-token-123", VERIFICATION_TIME * 60L);
            verify(response).sendRedirect(contains("signup"));
        }

        @Test
        @DisplayName("신규 네이버 회원 - RefreshToken 포함하여 저장")
        void newNaverMember_SavesWithRefreshToken() throws IOException {
            setupOAuth2Authentication("naver");

            when(memberRepository.findBySocialIdNaverAndIsDeletedFalse(TEST_SOCIAL_ID))
                    .thenReturn(Optional.empty());
            when(tokenProvider.createSocialTempToken(anyString(), anyString(), anyString()))
                    .thenReturn("temp-token-123");
            doNothing().when(redisAuthService).saveTempSocialInfo(anyString(), anyString(), anyString(), anyString());
            doNothing().when(cookieService).setSocialTempTokenCookie(any(), anyString(), anyLong());
            doNothing().when(response).sendRedirect(anyString());

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(redisAuthService).saveTempSocialInfo(anyString(), eq("naver"), eq(TEST_SOCIAL_ID), eq(TEST_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("신규 구글 회원 - RefreshToken 포함하여 저장")
        void newGoogleMember_SavesWithRefreshToken() throws IOException {
            setupOAuth2Authentication("google");

            when(memberRepository.findBySocialIdGoogleAndIsDeletedFalse(TEST_SOCIAL_ID))
                    .thenReturn(Optional.empty());
            when(tokenProvider.createSocialTempToken(anyString(), anyString(), anyString()))
                    .thenReturn("temp-token-123");
            doNothing().when(redisAuthService).saveTempSocialInfo(anyString(), anyString(), anyString(), anyString());
            doNothing().when(cookieService).setSocialTempTokenCookie(any(), anyString(), anyLong());
            doNothing().when(response).sendRedirect(anyString());

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(redisAuthService).saveTempSocialInfo(anyString(), eq("google"), eq(TEST_SOCIAL_ID), eq(TEST_REFRESH_TOKEN));
        }
    }

    @Nested
    @DisplayName("AttributeExtractor 통합 테스트")
    class AttributeExtractorIntegrationTests {

        @Test
        @DisplayName("AttributeExtractor가 null이면 에러 로그 출력")
        void attributeExtractorNull_LogsError() throws IOException {
            when(oAuth2Token.getPrincipal()).thenReturn(oAuth2User);
            when(oAuth2Token.getAuthorizedClientRegistrationId()).thenReturn("kakao");
            when(attributeExtractors.get("kakaoAttributeExtractor")).thenReturn(null);

            handler.onAuthenticationSuccess(request, response, oAuth2Token);

            verify(attributeExtractors).get("kakaoAttributeExtractor");
            verify(memberRepository, never()).findBySocialIdKakaoAndIsDeletedFalse(anyString());
        }
    }

    /** OAuth2 인증 설정 헬퍼 메서드 */
    private void setupOAuth2Authentication(String registrationId) {
        when(oAuth2Token.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2Token.getAuthorizedClientRegistrationId()).thenReturn(registrationId);

        String extractorBeanName = registrationId + "AttributeExtractor";
        when(attributeExtractors.get(extractorBeanName)).thenReturn(attributeExtractor);
        when(attributeExtractor.extractSocialId(oAuth2User)).thenReturn(TEST_SOCIAL_ID);
        when(attributeExtractor.extractProfileImageUrl(oAuth2User)).thenReturn(TEST_PROFILE_URL);

        OAuth2Provider provider = OAuth2Provider.fromRegistrationId(registrationId);
        when(tokenExtractor.extractRefreshToken(oAuth2Token, provider)).thenReturn(TEST_REFRESH_TOKEN);
    }
}