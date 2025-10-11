package tetoandeggens.seeyouagainbe.global.constants;

public class AuthConstants {
    // JWT 토큰 관련
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ROLE_CLAIM = "role";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static final String LOGIN_URI = "/auth/login";
    public static final String LOGOUT_URI = "/auth/logout";
    public static final String POST_METHOD = "POST";

    private AuthConstants() {}
}