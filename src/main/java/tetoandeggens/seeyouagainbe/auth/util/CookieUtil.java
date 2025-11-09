package tetoandeggens.seeyouagainbe.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

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

    public static void setCookie(HttpServletResponse response, String cookieName, String value, long maxAgeInSeconds) {
        addCookie(response, cookieName, value, maxAgeInSeconds);
    }

    public static void deleteCookie(HttpServletResponse response, String cookieName) {
        addCookie(response, cookieName, null, 0);
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
