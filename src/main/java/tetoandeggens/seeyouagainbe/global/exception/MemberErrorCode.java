package tetoandeggens.seeyouagainbe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 회원 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "회원을 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "MEMBER_101", "이미 사용 중인 아이디입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "MEMBER_102", "이미 사용 중인 휴대폰 번호입니다."),
    DUPLICATE_SOCIAL_ID(HttpStatus.CONFLICT, "MEMBER_103", "이미 가입된 소셜 계정입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}