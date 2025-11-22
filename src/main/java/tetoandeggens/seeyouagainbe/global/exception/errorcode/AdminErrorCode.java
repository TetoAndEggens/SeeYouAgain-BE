package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    VIOLATION_NOT_FOUND("ADMIN_001", "신고 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_PROCESSED_VIOLATION("ADMIN_002", "이미 처리된 신고입니다.", HttpStatus.BAD_REQUEST),
    ADMIN_PERMISSION_DENIED("ADMIN_003", "관리자 권한이 필요합니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}