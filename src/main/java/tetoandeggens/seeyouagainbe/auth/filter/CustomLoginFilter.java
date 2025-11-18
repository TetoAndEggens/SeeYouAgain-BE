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
import tetoandeggens.seeyouagainbe.auth.service.CookieService;
import tetoandeggens.seeyouagainbe.auth.service.RedisAuthService;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;
import tetoandeggens.seeyouagainbe.global.constants.AuthCommonConstants;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.*;

public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final CookieService cookieService;
    private final RedisAuthService redisAuthService;
    private final ObjectMapper objectMapper;

    public CustomLoginFilter(
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider,
            CookieService cookieService,
            RedisAuthService redisAuthService,
            ObjectMapper objectMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.cookieService = cookieService;
        this.redisAuthService = redisAuthService;
        this.objectMapper = objectMapper;
        setFilterProcessesUrl(AuthCommonConstants.LOGIN_URI);
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response)
            throws AuthenticationException {

        if (!AuthCommonConstants.LOGIN_URI.equals(request.getRequestURI()) ||
                !AuthCommonConstants.POST_METHOD.equals(request.getMethod())) {
            throw new CustomException(INVALID_LOGIN_REQUEST);
        }

        LoginRequest loginRequest;
        try {
            String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            loginRequest = objectMapper.readValue(messageBody, LoginRequest.class);
        } catch (IOException e) {
            throw new CustomException(INVALID_LOGIN_REQUEST);
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
        Long memberId = userDetails.getMemberId();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new CustomException(ACCESS_DENIED));

        UserTokenResponse loginToken = tokenProvider.createLoginToken(uuid, userDetails.getRole());

        cookieService.setAccessTokenCookie(
                response,
                loginToken.accessToken(),
                tokenProvider.getAccessTokenExpirationSec()
        );
        cookieService.setRefreshTokenCookie(
                response,
                loginToken.refreshToken(),
                tokenProvider.getRefreshTokenExpirationSec()
        );

        redisAuthService.saveRefreshToken(
                uuid,
                loginToken.refreshToken(),
                tokenProvider.getRefreshTokenExpirationMs()
        );

        redisAuthService.saveMemberId(
                uuid,
                memberId,
                tokenProvider.getRefreshTokenExpirationMs()
        );

        LoginResponse loginResponse = LoginResponse.builder()
                .uuid(userDetails.getUuid())
                .role(role)
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
        ResponseUtil.writeErrorResponse(response, objectMapper, WRONG_ID_PW);
    }
}
