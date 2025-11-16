package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ViolationErrorCode implements ErrorCode {

    VIOLATION_TARGET_REQUIRED("VIOLATION_001", "boardId 또는 chatRoomId 중 하나는 필수입니다.", HttpStatus.BAD_REQUEST),
    VIOLATION_TARGET_CONFLICT("VIOLATION_002", "boardId와 chatRoomId는 동시에 입력할 수 없습니다.", HttpStatus.BAD_REQUEST),
    REPORTER_NOT_FOUND("VIOLATION_003", "신고자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOARD_NOT_FOUND("VIOLATION_004","게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHATROOM_NOT_FOUND("VIOLATION_005","채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_VIOLATION("VIOLATION_006","이미 신고한 내역이 존재합니다.", HttpStatus.CONFLICT),
    SELF_REPORT_NOT_ALLOWED("VIOLATION_007","자기 자신을 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_CHAT_REPORT("VIOLATION_008","채팅방 참여자만 신고할 수 있습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}