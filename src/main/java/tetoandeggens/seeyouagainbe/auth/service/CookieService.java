package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;

import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.*;

@Service
@RequiredArgsConstructor
public class CookieService {

    private final CookieUtil cookieUtil;

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken, long expirationSec) {
        cookieUtil.setCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, expirationSec);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long expirationSec) {
        cookieUtil.setCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, expirationSec);
    }

    public void setSocialTempTokenCookie(HttpServletResponse response, String tempToken, long expirationSec) {
        cookieUtil.setCookie(response, SOCIAL_TEMP_TOKEN, tempToken, expirationSec);
    }

    public String resolveAccessToken(HttpServletRequest request) {
        return cookieUtil.resolveCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return cookieUtil.resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    public String resolveSocialTempToken(HttpServletRequest request) {
        return cookieUtil.resolveCookieValue(request, SOCIAL_TEMP_TOKEN);
    }

    public void deleteTempTokenCookie(HttpServletResponse response) {
        cookieUtil.deleteCookie(response, SOCIAL_TEMP_TOKEN);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        cookieUtil.deleteCookie(response, ACCESS_TOKEN_COOKIE_NAME);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        cookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    public void deleteAllAuthCookies(HttpServletResponse response) {
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
    }
}
