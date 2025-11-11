package tetoandeggens.seeyouagainbe.auth.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("CookieUtil 유틸 클래스 테스트")
class CookieUtilTest {

    @Test
    @DisplayName("쿠키를 성공적으로 설정한다")
    void setCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String cookieName = AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME;
        String value = "sample_access_token";
        long maxAgeInSeconds = 3600;

        CookieUtil.setCookie(response, cookieName, value, maxAgeInSeconds);

        Cookie cookie = response.getCookie(cookieName);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(value);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo((int) maxAgeInSeconds);
    }

    @Test
    @DisplayName("요청 쿠키에서 지정한 이름의 쿠키를 성공적으로 추출한다")
    void resolveCookieValue_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie cookie = new Cookie(AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME, "test_access_token");
        request.setCookies(cookie);

        String result = CookieUtil.resolveCookieValue(request, AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME);

        assertThat(result).isEqualTo("test_access_token");
    }

    @Test
    @DisplayName("요청 쿠키에 해당 이름이 없으면 null을 반환한다")
    void resolveCookieValue_ReturnsNull_WhenNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "value"));

        String result = CookieUtil.resolveCookieValue(request, AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("쿠키를 성공적으로 삭제한다")
    void deleteCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtil.deleteCookie(response, AuthCommonConstants.REFRESH_TOKEN_COOKIE_NAME);

        Cookie cookie = response.getCookie(AuthCommonConstants.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isZero();
    }
}