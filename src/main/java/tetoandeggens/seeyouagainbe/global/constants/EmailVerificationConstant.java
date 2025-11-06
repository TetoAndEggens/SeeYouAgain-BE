package tetoandeggens.seeyouagainbe.global.constants;

public class EmailVerificationConstant {

    public static final int VERIFICATION_TIME = 5; // (분)
    public static final String VERIFICATION_CODE_PATTERN = "\\b(\\d{6})\\b";
    public static final String PREFIX_VERIFICATION_CODE = "verify:phone:code:";
    public static final String PREFIX_VERIFICATION_TIME = "verify:phone:time:";
    public static final String VERIFIED = "verify:phone:verified:";

    public static final String PREFIX_SOCIAL_VERIFICATION_CODE = "social:phone:code:";
    public static final String PREFIX_SOCIAL_VERIFICATION_TIME = "social:phone:time:";
    public static final String PREFIX_SOCIAL_VERIFIED = "social:phone:verified:";
    public static final String PREFIX_SOCIAL_PROVIDER = "social:phone:provider:";
    public static final String PREFIX_SOCIAL_ID = "social:phone:socialid:";
    public static final String PREFIX_SOCIAL_PROFILE = "social:phone:profile:";
    public static final String PREFIX_SOCIAL_REFRESH_TOKEN = "social:phone:refreshtoken:";


    public static final String IMAP_PROTOCOL = "imaps";
    public static final String INBOX = "INBOX";
    public static final String AT = "@";
    public static final String TEXT = "text/plain";
    public static final String MULTIPART = "multipart/*";
    public static final String TXT = ".txt";

    private EmailVerificationConstant() {}
}
