package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 로그인/회원 관련
    WRONG_ID_PW("AUTH_001", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    DUPLICATED_LOGIN_ID("AUTH_002", "이미 사용 중인 로그인 아이디입니다.", HttpStatus.CONFLICT),
    MEMBER_NOT_FOUND("AUTH_003", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 휴대폰 인증 관련
    PHONE_NUMBER_DUPLICATED("AUTH_004", "이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT),
    INVALID_VERIFICATION_CODE("AUTH_005", "유효하지 않은 인증 코드입니다.", HttpStatus.BAD_REQUEST),
    PHONE_NOT_VERIFIED("AUTH_006","휴대폰 인증이 완료되지 않았습니다.", HttpStatus.FORBIDDEN),

    // 토큰 관련
    INVALID_TOKEN("AUTH_007", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_008", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("AUTH_009", "토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_JWT_SIGNATURE("AUTH_010", "잘못된 JWT 서명입니다.",HttpStatus.UNAUTHORIZED),
    UNSUPPORTED_JWT("AUTH_011", "지원되지 않는 JWT 토큰입니다.",HttpStatus.UNAUTHORIZED),
    EMPTY_JWT_CLAIMS("AUTH_012", "JWT 클레임 문자열이 비어 있습니다.",HttpStatus.UNAUTHORIZED),
    INCORRECT_CLAIM_TOKEN("AUTH_013", "토큰의 클레임 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("AUTH_014", "리프레시 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISMATCH("AUTH_015", "Refresh Token이 일치하지 않습니다.",HttpStatus.UNAUTHORIZED),

    // 권한 관련
    ACCESS_DENIED("AUTH_016", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 요청 관련
    INVALID_LOGIN_REQUEST("AUTH_017", "잘못된 로그인 요청입니다.", HttpStatus.BAD_REQUEST),

    // OAuth2 관련
    UNSUPPORTED_OAUTH2_PROVIDER("AUTH_018", "지원하지 않는 OAuth2 제공자입니다.", HttpStatus.BAD_REQUEST),
    INVALID_OAUTH2_STATE("AUTH_019", "유효하지 않은 OAuth2 state 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_OAUTH2_STATE("AUTH_020", "만료된 OAuth2 state 토큰입니다.", HttpStatus.UNAUTHORIZED),
    OAUTH2_TOKEN_EXCHANGE_FAILED("AUTH_021", "OAuth2 토큰 교환에 실패했습니다.", HttpStatus.BAD_REQUEST),
    OAUTH2_USER_INFO_FETCH_FAILED("AUTH_022", "OAuth2 사용자 정보 조회에 실패했습니다.", HttpStatus.BAD_REQUEST),
    REAUTH_TOKEN_NOT_FOUND("AUTH_023", "재인증 토큰이 없습니다. 소셜 로그인을 다시 진행해주세요.", HttpStatus.BAD_REQUEST),
    SOCIAL_TOKEN_REFRESH_FAILED("AUTH_024", "소셜 로그인 토큰 갱신에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);



    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
