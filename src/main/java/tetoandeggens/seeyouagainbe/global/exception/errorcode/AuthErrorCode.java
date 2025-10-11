package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    WRONG_ID_PW("AUTH_001", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    DUPLICATED_LOGIN_ID("AUTH_002", "이미 사용 중인 로그인 아이디입니다.", HttpStatus.CONFLICT),
    DUPLICATED_PHONE_NUMBER("AUTH_003", "이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT),
    REFRESH_TOKEN_NOT_FOUND("AUTH_004", "리프레시 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_005", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_006", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_007", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    TOKEN_NOT_FOUND("AUTH_008", "토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_009", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INCORRECT_CLAIM_TOKEN("AUTH_010", "토큰의 클레임 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("AUTH_011", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}