package tetoandeggens.seeyouagainbe.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {

    private static final String LOGOUT_SUCCESS_MESSAGE = "로그아웃 성공";

    private final TokenProvider tokenProvider;
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

        String accessToken = tokenProvider.resolveAccessToken(request);
        String refreshToken = tokenProvider.resolveRefreshToken(request);

        if (accessToken == null || refreshToken == null) {
            ResponseUtil.writeErrorResponse(response, objectMapper, AuthErrorCode.TOKEN_NOT_FOUND);
            return;
        }

        try {
            Claims claims = tokenProvider.getClaimsFromToken(accessToken);
            String uuid = claims.getSubject();

            tokenProvider.deleteRefreshToken(uuid);
            tokenProvider.deleteRefreshTokenCookie(response);

            ResponseUtil.writeSuccessResponse(response, objectMapper, LOGOUT_SUCCESS_MESSAGE, HttpStatus.OK);
        } catch (Exception e) {
            ResponseUtil.writeErrorResponse(response, objectMapper, AuthErrorCode.INVALID_TOKEN);
        }
    }
}
