package tetoandeggens.seeyouagainbe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 401 Unauthorized
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_001", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    INCORRECT_CLAIM_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "잘못된 토큰 정보입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 Refresh Token입니다."),
    WRONG_ID_PW(HttpStatus.UNAUTHORIZED, "AUTH_006", "아이디 또는 비밀번호가 일치하지 않습니다."),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_101", "접근 권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_201", "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}