package tetoandeggens.seeyouagainbe.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;

public class CustomUserLoginFilter extends CustomUsernamePasswordAuthenticationFilter {

    private static final String LOGIN_URI = "/api/auth/login";

    public CustomUserLoginFilter(
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider,
            ObjectMapper objectMapper
    ) {
        super(authenticationManager, tokenProvider, objectMapper);
        this.setFilterProcessesUrl(LOGIN_URI);
    }
}
