package tetoandeggens.seeyouagainbe.global.constants;

public class AuthVerificationConstants {

    public static final int VERIFICATION_TIME = 10; // (분)
    public static final String VERIFICATION_CODE_PATTERN = "\\b(\\d{6})\\b";

    // JWT Token 관리 (RefreshToken + MemberId)
    public static final String PREFIX_REFRESH_TOKEN = "refresh:";
    public static final String PREFIX_MEMBER_ID = "member:";

    // 일반 유저로 회원가입 시, 필요한 휴대폰 인증관련 key값
    public static final String PREFIX_VERIFICATION_CODE = "verify:phone:code:";
    public static final String PREFIX_VERIFICATION_TIME = "verify:phone:time:";
    public static final String VERIFIED = "verify:phone:verified:";

    // 소셜연동 및 소셜 연동으로 회원가입 시, 필요한 휴대폰 인증 관련 key값
    public static final String PREFIX_SOCIAL_VERIFICATION_CODE = "social:phone:code:";
    public static final String PREFIX_SOCIAL_VERIFICATION_TIME = "social:phone:time:";
    public static final String PREFIX_SOCIAL_VERIFIED = "social:phone:verified:";
    public static final String PREFIX_SOCIAL_PROVIDER = "social:phone:provider:";
    public static final String PREFIX_SOCIAL_ID = "social:phone:socialid:";
    public static final String PREFIX_SOCIAL_TEMP_UUID = "social:phone:tempuuid:";

    // OAuth2 임시 저장 (소셜 로그인 성공 직후)
    public static final String PREFIX_TEMP_SOCIAL_ID = "temp:social:id:";
    public static final String PREFIX_TEMP_SOCIAL_REFRESH = "temp:social:refresh:";
    public static final String PREFIX_TEMP_SOCIAL_PROVIDER = "temp:social:provider:";

    // 이메일 관련
    public static final String IMAP_PROTOCOL = "imaps";
    public static final String INBOX = "INBOX";
    public static final String AT = "@";
    public static final String TEXT = "text/plain";
    public static final String MULTIPART = "multipart/*";
    public static final String TXT = ".txt";

    private AuthVerificationConstants() {}
}
