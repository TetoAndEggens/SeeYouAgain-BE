package tetoandeggens.seeyouagainbe.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.dto.request.LoginRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.LoginResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class CustomUserLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public CustomUserLoginFilter(AuthenticationManager authenticationManager,
                                 TokenProvider tokenProvider,
                                 ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        LoginRequest loginRequest;
        try {
            String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            loginRequest = objectMapper.readValue(messageBody, LoginRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginRequest.loginId(), loginRequest.password(), null);
        return authenticationManager.authenticate(authToken);
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authentication
    ) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uuid = userDetails.getUuid();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new RuntimeException("권한이 식별되지 않은 사용자입니다: " + uuid));

        UserTokenResponse loginToken = tokenProvider.createLoginToken(uuid, userDetails.getRole());

        // RefreshToken을 쿠키에 저장
        tokenProvider.setRefreshTokenCookie(response, loginToken.refreshToken());

        LoginResponse loginResponse = LoginResponse.builder()
                .uuid(userDetails.getUuid())
                .role(role)
                .accessToken(loginToken.accessToken())
                .build();

        ResponseUtil.writeSuccessResponseWithHeaders(
                response,
                objectMapper,
                loginResponse,
                HttpStatus.OK
        );
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed
    ) throws IOException {
        ResponseUtil.writeErrorResponse(response, objectMapper, AuthErrorCode.WRONG_ID_PW);
    }
}
