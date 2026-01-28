package tetoandeggens.seeyouagainbe.auth.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tetoandeggens.seeyouagainbe.auth.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;

import java.io.IOException;
import java.util.List;

import static tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants.REDIRECT_URI_PARAM_COOKIE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository authRequestRepository;
    private final CookieUtil cookieUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final List<String> ALLOWED_LOCAL_ORIGINS = List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    );

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        authRequestRepository.removeAuthorizationRequestCookies(request, response);

        log.error("[OAuth2Failure] 소셜 로그인 실패: {}", exception.getMessage());

        String targetFrontendUrl = determineTargetUrl(request);

        String redirectUrl = UriComponentsBuilder.fromUriString(targetFrontendUrl)
                .path("/auth/error")
                .queryParam("message", "social_login_failed")
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String determineTargetUrl(HttpServletRequest request) {
        String redirectUriFromCookie = cookieUtil.resolveCookieValue(request, REDIRECT_URI_PARAM_COOKIE_NAME);

        if (redirectUriFromCookie != null && ALLOWED_LOCAL_ORIGINS.contains(redirectUriFromCookie)) {
            return redirectUriFromCookie;
        }

        return frontendUrl;
    }
}