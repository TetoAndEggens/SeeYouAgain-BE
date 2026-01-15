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

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken, long expirationSec) {
        CookieUtil.setCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, expirationSec);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long expirationSec) {
        CookieUtil.setCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, expirationSec);
    }

    public void setSocialTempTokenCookie(HttpServletResponse response, String tempToken, long expirationSec) {
        CookieUtil.setCookie(response, SOCIAL_TEMP_TOKEN, tempToken, expirationSec);
    }

    public String resolveAccessToken(HttpServletRequest request) {
        return CookieUtil.resolveCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return CookieUtil.resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    public String resolveSocialTempToken(HttpServletRequest request) {
        return CookieUtil.resolveCookieValue(request, SOCIAL_TEMP_TOKEN);
    }

    public void deleteTempTokenCookie(HttpServletResponse response) {
        CookieUtil.deleteCookie(response, SOCIAL_TEMP_TOKEN);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        CookieUtil.deleteCookie(response, ACCESS_TOKEN_COOKIE_NAME);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        CookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    public void deleteAllAuthCookies(HttpServletResponse response) {
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
    }
}
