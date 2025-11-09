package tetoandeggens.seeyouagainbe.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.service.CookieService;
import tetoandeggens.seeyouagainbe.auth.service.RedisAuthService;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;

import java.io.IOException;

import static tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.*;

@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final CookieService cookieService;
    private final RedisAuthService redisAuthService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();

        if (!AuthConstants.LOGOUT_URI.equals(requestURI) || !AuthConstants.POST_METHOD.equals(requestMethod)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = cookieService.resolveAccessToken(request);
        String refreshToken = cookieService.resolveRefreshToken(request);

        if (accessToken == null || refreshToken == null) {
            ResponseUtil.writeErrorResponse(response, objectMapper, TOKEN_NOT_FOUND);
            return;
        }

        try {
            Claims claims = tokenProvider.parseClaims(accessToken);
            String uuid = claims.getSubject();

            redisAuthService.deleteRefreshToken(uuid);
            cookieService.deleteAllAuthCookies(response);

            ResponseUtil.writeNoContent(response, objectMapper, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            ResponseUtil.writeErrorResponse(response, objectMapper, INVALID_TOKEN);
        }
    }
}
