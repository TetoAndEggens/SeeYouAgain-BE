package tetoandeggens.seeyouagainbe.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.util.ResponseUtil;
import tetoandeggens.seeyouagainbe.global.exception.AuthErrorCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ResponseUtil.writeErrorResponse(response, objectMapper, AuthErrorCode.TOKEN_NOT_FOUND);
    }
}
