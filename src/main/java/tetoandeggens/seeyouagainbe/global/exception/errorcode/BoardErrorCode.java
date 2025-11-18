package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode implements ErrorCode {

	BOARD_NOT_FOUND("BOARD_001", "게시물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INVALID_CONTENT_TYPE("BOARD_002", "유효하지 않은 게시물 타입입니다.", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}