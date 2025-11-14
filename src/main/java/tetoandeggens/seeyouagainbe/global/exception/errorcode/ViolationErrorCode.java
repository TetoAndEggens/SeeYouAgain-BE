package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ViolationErrorCode implements ErrorCode {

    REPORTER_NOT_FOUND("VIOLATION_001", "신고자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOARD_NOT_FOUND("VIOLATION_002","게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHATROOM_NOT_FOUND("VIOLATION_003","채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_VIOLATION("VIOLATION_004","이미 신고한 내역이 존재합니다.", HttpStatus.CONFLICT),
    SELF_REPORT_NOT_ALLOWED("VIOLATION_005","자기 자신을 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_CHAT_REPORT("VIOLATION_006","채팅방 참여자만 신고할 수 있습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}