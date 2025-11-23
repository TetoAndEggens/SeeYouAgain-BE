package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

	BOARD_NOT_FOUND("CHAT_001", "게시물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	SENDER_NOT_FOUND("CHAT_002", "발신자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	RECEIVER_NOT_FOUND("CHAT_003", "수신자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	CHAT_ROOM_NOT_FOUND("CHAT_004", "채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	CHAT_MESSAGE_NOT_FOUND("CHAT_005", "채팅 메시지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	CHAT_FORBIDDEN("CHAT_006", "채팅방에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}
