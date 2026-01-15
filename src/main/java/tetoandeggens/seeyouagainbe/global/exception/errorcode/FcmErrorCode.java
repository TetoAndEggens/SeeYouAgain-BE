package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FcmErrorCode implements ErrorCode {

    TOKEN_NOT_FOUND("FCM-001", "FCM 토큰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_TOKEN("FCM-002", "유효하지 않은 FCM 토큰입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}