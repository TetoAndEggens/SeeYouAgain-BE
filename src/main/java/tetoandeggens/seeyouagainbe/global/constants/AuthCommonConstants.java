package tetoandeggens.seeyouagainbe.global.constants;

public class AuthCommonConstants {
    public static final String ROLE_CLAIM = "role";
    public static final String SOCIAL_TEMP_TOKEN = "socialTempToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    public static final String LOGIN_URI = "/auth/login";
    public static final String LOGOUT_URI = "/auth/logout";
    public static final String POST_METHOD = "POST";

    // ===== JWT claim keys =====
    public static final String CLAIM_PROVIDER = "provider";
    public static final String CLAIM_PROFILE_IMAGE_URL = "profileImageUrl";
    public static final String CLAIM_TEMP_UUID = "tempUuid";
    public static final String CLAIM_TYPE = "type";

    // ===== JWT claim values =====
    public static final String CLAIM_TYPE_SOCIAL_TEMP = "social_temp";

    // ===== OAuth2 HttpCookieOAuth2AuthorizationRequestRepository =====
    public static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    public static final int COOKIE_EXPIRE_SECONDS = 180;  // 3분

    private AuthCommonConstants() {}
}