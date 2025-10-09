package tetoandeggens.seeyouagainbe.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilter {

    private static final String LOGOUT_URL = "/api/auth/logout";

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        if (!request.getRequestURI().equals(LOGOUT_URL) || !HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = tokenProvider.resolveAccessToken(request);
        Claims claims = tokenProvider.getClaimsFromToken(accessToken);
        String userId = claims.getSubject();

        tokenProvider.deleteRefreshToken(userId);

        ResponseUtil.writeNoContent(response, objectMapper, HttpStatus.OK);
    }
}
