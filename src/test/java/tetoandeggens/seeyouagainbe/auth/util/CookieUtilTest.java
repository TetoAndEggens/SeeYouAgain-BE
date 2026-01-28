package tetoandeggens.seeyouagainbe.auth.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tetoandeggens.seeyouagainbe.global.config.CookieProperties;
import tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("CookieUtil 유틸 클래스 테스트")
class CookieUtilTest {

    private CookieUtil cookieUtil;
    private CookieProperties cookieProperties;

    @BeforeEach
    void setUp() {
        cookieProperties = new CookieProperties();
        cookieProperties.setDomain(".seeyouagain.store");
        cookieProperties.setSecure(true);
        cookieProperties.setSameSite("None");

        cookieUtil = new CookieUtil(cookieProperties);
    }

    @Test
    @DisplayName("쿠키를 성공적으로 설정한다")
    void setCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String cookieName = AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME;
        String value = "sample_access_token";
        long maxAgeInSeconds = 3600;

        cookieUtil.setCookie(response, cookieName, value, maxAgeInSeconds);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains(cookieName + "=" + value);
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("SameSite=None");
        assertThat(setCookieHeader).contains("Domain=.seeyouagain.store");
    }

    @Test
    @DisplayName("localhost 환경에서는 Domain이 설정되지 않는다")
    void setCookie_LocalhostNoDomain() {
        // localhost용 설정
        cookieProperties.setDomain("localhost");
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");
        cookieUtil = new CookieUtil(cookieProperties);

        MockHttpServletResponse response = new MockHttpServletResponse();
        String cookieName = AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME;
        String value = "sample_access_token";
        long maxAgeInSeconds = 3600;

        cookieUtil.setCookie(response, cookieName, value, maxAgeInSeconds);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains(cookieName + "=" + value);
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).doesNotContain("Secure");
        assertThat(setCookieHeader).contains("SameSite=Lax");
        assertThat(setCookieHeader).doesNotContain("Domain=");
    }

    @Test
    @DisplayName("요청 쿠키에서 지정한 이름의 쿠키를 성공적으로 추출한다")
    void resolveCookieValue_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie cookie = new Cookie(AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME, "test_access_token");
        request.setCookies(cookie);

        String result = cookieUtil.resolveCookieValue(request, AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME);

        assertThat(result).isEqualTo("test_access_token");
    }

    @Test
    @DisplayName("요청 쿠키에 해당 이름이 없으면 null을 반환한다")
    void resolveCookieValue_ReturnsNull_WhenNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "value"));

        String result = cookieUtil.resolveCookieValue(request, AuthCommonConstants.ACCESS_TOKEN_COOKIE_NAME);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("쿠키를 성공적으로 삭제한다")
    void deleteCookie_Success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.deleteCookie(response, AuthCommonConstants.REFRESH_TOKEN_COOKIE_NAME);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains(AuthCommonConstants.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(setCookieHeader).contains("Max-Age=0");
    }
}