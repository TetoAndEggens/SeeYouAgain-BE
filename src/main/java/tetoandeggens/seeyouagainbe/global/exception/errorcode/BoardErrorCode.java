package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode implements ErrorCode {

	BOARD_NOT_FOUND("BOARD_001", "게시물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INVALID_CONTENT_TYPE("BOARD_002", "유효하지 않은 게시물 타입입니다.", HttpStatus.BAD_REQUEST),
	BOARD_FORBIDDEN("BOARD_003", "게시물에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
	BOARD_ALREADY_DELETED("BOARD_004", "이미 삭제된 게시물입니다.", HttpStatus.BAD_REQUEST),
	INVALID_IMAGE_IDS("BOARD_005", "유효하지 않은 이미지 ID입니다.", HttpStatus.BAD_REQUEST),
	INVALID_TAG_IDS("BOARD_006", "유효하지 않은 태그 ID입니다.", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}