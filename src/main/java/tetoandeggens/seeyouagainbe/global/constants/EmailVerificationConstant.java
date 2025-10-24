package tetoandeggens.seeyouagainbe.global.constants;

public class EmailVerificationConstant {

    public static final int VERIFICATION_TIME = 5; // (분)
    public static final String VERIFICATION_CODE_PATTERN = "\\b(\\d{6})\\b";
    public static final String PREFIX_VERIFICATION_CODE = "phone:code:";
    public static final String PREFIX_VERIFICATION_TIME = "phone:time:";
    public static final String VERIFIED = "verified";

    public static final String IMAP_PROTOCOL = "imaps";
    public static final String INBOX = "INBOX";
    public static final String AT = "@";
    public static final String TEXT = "text/plain";
    public static final String MULTIPART = "multipart/*";
    public static final String TXT = ".txt";

    private EmailVerificationConstant() {}
}
