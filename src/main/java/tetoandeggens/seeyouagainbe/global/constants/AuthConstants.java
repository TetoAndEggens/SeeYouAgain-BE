package tetoandeggens.seeyouagainbe.global.constants;

public class AuthConstants {
    // JWT 토큰 관련
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ROLE_CLAIM = "role";

    // 시간 관련 상수
    public static final int SECOND_IN_MILLISECONDS = 1000;
    public static final int MINUTE_IN_MILLISECONDS = 60 * SECOND_IN_MILLISECONDS;
    public static final int HOUR_IN_MILLISECONDS = 60 * MINUTE_IN_MILLISECONDS;
    public static final long DAY_IN_MILLISECONDS = 24 * HOUR_IN_MILLISECONDS;

    private AuthConstants() {}
}