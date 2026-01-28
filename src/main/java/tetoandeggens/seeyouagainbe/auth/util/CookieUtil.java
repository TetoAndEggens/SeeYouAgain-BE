package tetoandeggens.seeyouagainbe.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.global.config.CookieProperties;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private static final boolean HTTP_ONLY = true;
    private static final String PATH = "/";

    private final CookieProperties cookieProperties;

    public String resolveCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void setCookie(HttpServletResponse response, String cookieName, String value, long maxAgeInSeconds) {
        addCookie(response, cookieName, value, maxAgeInSeconds);
    }

    public void deleteCookie(HttpServletResponse response, String cookieName) {
        addCookie(response, cookieName, null, 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(HTTP_ONLY)
                .secure(cookieProperties.isSecure())
                .path(PATH)
                .maxAge(maxAgeSeconds)
                .sameSite(cookieProperties.getSameSite());

        // localhost가 아닐 때만 domain 설정
        if (!"localhost".equals(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain());
        }

        ResponseCookie cookie = builder.build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
