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
	CHAT_FORBIDDEN("CHAT_006", "채팅방에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
	MESSAGE_ENCRYPTION_FAILED("CHAT_007", "메시지 암호화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	MESSAGE_DECRYPTION_FAILED("CHAT_008", "메시지 복호화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_ENCRYPTION_KEY("CHAT_009", "암호화 키 설정이 올바르지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	MESSAGE_NOT_FOUND("CHAT_010", "메시지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	NOT_MESSAGE_RECEIVER("CHAT_011", "메시지 수신자가 아닙니다.", HttpStatus.FORBIDDEN),
	CANNOT_CHAT_WITH_SELF("CHAT_012", "자기 자신과 채팅할 수 없습니다.", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}
