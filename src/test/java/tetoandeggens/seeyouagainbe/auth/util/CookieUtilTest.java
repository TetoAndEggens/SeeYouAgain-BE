package tetoandeggens.seeyouagainbe.auth.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("CookieUtil 유틸 클래스 테스트")
class CookieUtilTest {
    @Test
    @DisplayName("AccessToken 쿠키를 성공적으로 설정한다")
    void setAccessTokenCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String accessToken = "sample_access_token";
        long maxAgeInSeconds = 3600;

        CookieUtil.setAccessTokenCookie(response, accessToken, maxAgeInSeconds);

        Cookie cookie = response.getCookie(AuthConstants.ACCESS_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(accessToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo((int) maxAgeInSeconds);
    }

    @Test
    @DisplayName("요청 쿠키에서 AccessToken을 성공적으로 추출한다")
    void resolveAccessTokenFromCookie_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie accessCookie = new Cookie(AuthConstants.ACCESS_TOKEN_COOKIE_NAME, "test_access_token");
        request.setCookies(accessCookie);

        String result = CookieUtil.resolveAccessTokenFromCookie(request);

        assertThat(result).isEqualTo("test_access_token");
    }

    @Test
    @DisplayName("AccessToken 쿠키가 없으면 null을 반환한다")
    void resolveAccessTokenFromCookie_ReturnsNull_WhenNoCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "value"));

        String result = CookieUtil.resolveAccessTokenFromCookie(request);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("AccessToken 쿠키를 성공적으로 삭제한다")
    void deleteAccessTokenCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtil.deleteAccessTokenCookie(response);

        Cookie cookie = response.getCookie(AuthConstants.ACCESS_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("Refresh Token 쿠키를 성공적으로 설정한다")
    void setRefreshTokenCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String refreshToken = "sample_refresh_token";
        long maxAgeInSeconds = 3600;

        CookieUtil.setRefreshTokenCookie(response, refreshToken, maxAgeInSeconds);

        Cookie cookie = response.getCookie(AuthConstants.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(refreshToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo((int) maxAgeInSeconds);
    }

    @Test
    @DisplayName("요청 쿠키에서 Refresh Token을 성공적으로 추출한다")
    void resolveRefreshTokenFromCookie_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie refreshCookie = new Cookie(AuthConstants.REFRESH_TOKEN_COOKIE_NAME, "test_refresh_token");
        request.setCookies(refreshCookie);

        String result = CookieUtil.resolveRefreshTokenFromCookie(request);

        assertThat(result).isEqualTo("test_refresh_token");
    }

    @Test
    @DisplayName("Refresh Token 쿠키가 없으면 null을 반환한다")
    void resolveRefreshTokenFromCookie_ReturnsNull_WhenNoCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "value"));

        String result = CookieUtil.resolveRefreshTokenFromCookie(request);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Refresh Token 쿠키를 성공적으로 삭제한다")
    void deleteRefreshTokenCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtil.deleteRefreshTokenCookie(response);

        Cookie cookie = response.getCookie(AuthConstants.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isZero();
    }
}