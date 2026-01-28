package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieService 단위 테스트")
class CookieServiceTest {

    @InjectMocks
    private CookieService cookieService;

    @Mock
    private CookieUtil cookieUtil;

    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private static final String TEST_SOCIAL_TEMP_TOKEN = "test-social-temp-token";
    private static final long TEST_EXPIRATION_SEC = 3600L;

    @Nested
    @DisplayName("Access Token 쿠키 관리 테스트")
    class AccessTokenCookieTests {

        @Test
        @DisplayName("Access Token 쿠키 설정 - 성공")
        void setAccessTokenCookie_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN, TEST_EXPIRATION_SEC);

            // then
            verify(cookieUtil, times(1)).setCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME,
                    TEST_ACCESS_TOKEN,
                    TEST_EXPIRATION_SEC
            );
        }

        @Test
        @DisplayName("Access Token 추출 - 성공")
        void resolveAccessToken_Success() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie(ACCESS_TOKEN_COOKIE_NAME, TEST_ACCESS_TOKEN));

            when(cookieUtil.resolveCookieValue(request, ACCESS_TOKEN_COOKIE_NAME))
                    .thenReturn(TEST_ACCESS_TOKEN);

            // when
            String result = cookieService.resolveAccessToken(request);

            // then
            assertThat(result).isEqualTo(TEST_ACCESS_TOKEN);
            verify(cookieUtil, times(1)).resolveCookieValue(
                    request,
                    ACCESS_TOKEN_COOKIE_NAME
            );
        }

        @Test
        @DisplayName("Access Token 추출 - 쿠키가 없으면 null 반환")
        void resolveAccessToken_ReturnsNull_WhenNoCookie() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();

            when(cookieUtil.resolveCookieValue(request, ACCESS_TOKEN_COOKIE_NAME))
                    .thenReturn(null);

            // when
            String result = cookieService.resolveAccessToken(request);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Access Token 쿠키 삭제 - 성공")
        void deleteAccessTokenCookie_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.deleteAccessTokenCookie(response);

            // then
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME
            );
        }
    }

    @Nested
    @DisplayName("Refresh Token 쿠키 관리 테스트")
    class RefreshTokenCookieTests {

        @Test
        @DisplayName("Refresh Token 쿠키 설정 - 성공")
        void setRefreshTokenCookie_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.setRefreshTokenCookie(response, TEST_REFRESH_TOKEN, TEST_EXPIRATION_SEC);

            // then
            verify(cookieUtil, times(1)).setCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME,
                    TEST_REFRESH_TOKEN,
                    TEST_EXPIRATION_SEC
            );
        }

        @Test
        @DisplayName("Refresh Token 추출 - 성공")
        void resolveRefreshToken_Success() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie(REFRESH_TOKEN_COOKIE_NAME, TEST_REFRESH_TOKEN));

            when(cookieUtil.resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME))
                    .thenReturn(TEST_REFRESH_TOKEN);

            // when
            String result = cookieService.resolveRefreshToken(request);

            // then
            assertThat(result).isEqualTo(TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Refresh Token 쿠키 삭제 - 성공")
        void deleteRefreshTokenCookie_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.deleteRefreshTokenCookie(response);

            // then
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME
            );
        }
    }

    @Nested
    @DisplayName("Social Temp Token 쿠키 관리 테스트")
    class SocialTempTokenCookieTests {

        @Test
        @DisplayName("Social Temp Token 쿠키 설정 - 성공")
        void setSocialTempTokenCookie_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.setSocialTempTokenCookie(response, TEST_SOCIAL_TEMP_TOKEN, TEST_EXPIRATION_SEC);

            // then
            verify(cookieUtil, times(1)).setCookie(
                    response,
                    SOCIAL_TEMP_TOKEN,
                    TEST_SOCIAL_TEMP_TOKEN,
                    TEST_EXPIRATION_SEC
            );
        }

        @Test
        @DisplayName("Social Temp Token 추출 - 성공")
        void resolveSocialTempToken_Success() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie(SOCIAL_TEMP_TOKEN, TEST_SOCIAL_TEMP_TOKEN));

            when(cookieUtil.resolveCookieValue(request, SOCIAL_TEMP_TOKEN))
                    .thenReturn(TEST_SOCIAL_TEMP_TOKEN);

            // when
            String result = cookieService.resolveSocialTempToken(request);

            // then
            assertThat(result).isEqualTo(TEST_SOCIAL_TEMP_TOKEN);
        }

        @Test
        @DisplayName("Social Temp Token 쿠키 삭제 - 성공")
        void deleteSocialTempTokenCookie_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.deleteTempTokenCookie(response);

            // then
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    SOCIAL_TEMP_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("모든 인증 쿠키 삭제 테스트")
    class DeleteAllAuthCookiesTests {

        @Test
        @DisplayName("모든 인증 쿠키 삭제 - 성공")
        void deleteAllAuthCookies_Success() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.deleteAllAuthCookies(response);

            // then
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME
            );
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME
            );
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("로그인 시나리오 - Access Token과 Refresh Token 모두 설정")
        void loginScenario_SetsBothTokens() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN, TEST_EXPIRATION_SEC);
            cookieService.setRefreshTokenCookie(response, TEST_REFRESH_TOKEN, TEST_EXPIRATION_SEC);

            // then
            verify(cookieUtil, times(1)).setCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME,
                    TEST_ACCESS_TOKEN,
                    TEST_EXPIRATION_SEC
            );
            verify(cookieUtil, times(1)).setCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME,
                    TEST_REFRESH_TOKEN,
                    TEST_EXPIRATION_SEC
            );
        }

        @Test
        @DisplayName("로그아웃 시나리오 - 모든 쿠키 삭제")
        void logoutScenario_DeletesAllCookies() {
            // given
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cookieService.deleteAllAuthCookies(response);

            // then
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    ACCESS_TOKEN_COOKIE_NAME
            );
            verify(cookieUtil, times(1)).deleteCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME
            );
        }

        @Test
        @DisplayName("소셜 로그인 시나리오 - Social Temp Token 설정 및 추출")
        void socialLoginScenario_SetAndResolveTempToken() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(cookieUtil.resolveCookieValue(request, SOCIAL_TEMP_TOKEN))
                    .thenReturn(TEST_SOCIAL_TEMP_TOKEN);

            // when
            cookieService.setSocialTempTokenCookie(response, TEST_SOCIAL_TEMP_TOKEN, TEST_EXPIRATION_SEC);
            String resolvedToken = cookieService.resolveSocialTempToken(request);

            // then
            assertThat(resolvedToken).isEqualTo(TEST_SOCIAL_TEMP_TOKEN);
            verify(cookieUtil, times(1)).setCookie(
                    response,
                    SOCIAL_TEMP_TOKEN,
                    TEST_SOCIAL_TEMP_TOKEN,
                    TEST_EXPIRATION_SEC
            );
        }
    }
}