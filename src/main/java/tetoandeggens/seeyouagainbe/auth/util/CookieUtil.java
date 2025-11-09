package tetoandeggens.seeyouagainbe.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;

public class CookieUtil {

    private static final String SAME_SITE = "Lax";
    private static final boolean SECURE = true;
    private static final boolean HTTP_ONLY = true;
    private static final String PATH = "/";

    public static String resolveCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void setSocialTempTokenCookie(HttpServletResponse response, String tempToken, int maxAge) {
        addCookie(response, AuthConstants.SOCIAL_TEMP_TOKEN, tempToken, maxAge);
    }

    public static void setAccessTokenCookie(HttpServletResponse response, String accessToken, long maxAgeInSeconds) {
        addCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, accessToken, maxAgeInSeconds);
    }

    public static void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeInSeconds) {
        addCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAgeInSeconds);
    }

    public static void deleteAccessTokenCookie(HttpServletResponse response) {
        addCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, "", 0);
    }

    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        addCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, "", 0);
    }

    private static void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(maxAgeSeconds)
                .sameSite(SAME_SITE)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private CookieUtil() {}
}
