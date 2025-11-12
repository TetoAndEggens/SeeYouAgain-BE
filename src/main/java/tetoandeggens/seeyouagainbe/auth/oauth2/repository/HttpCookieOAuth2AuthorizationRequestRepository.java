package tetoandeggens.seeyouagainbe.auth.oauth2.repository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;

import java.util.Base64;

import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.*;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String serialized = CookieUtil.resolveCookieValue(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        if (!StringUtils.hasText(serialized)) return null;
        byte[] bytes = Base64.getUrlDecoder().decode(serialized);
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        byte[] bytes = SerializationUtils.serialize(authorizationRequest);
        String serialized = Base64.getUrlEncoder().encodeToString(bytes);
        CookieUtil.setCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtil.setCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest existing = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return existing;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
