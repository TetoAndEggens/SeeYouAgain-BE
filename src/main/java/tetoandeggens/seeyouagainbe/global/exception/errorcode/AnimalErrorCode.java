package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalErrorCode implements ErrorCode {

	ANIMAL_NOT_FOUND("ANIMAL_001", "유기 동물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INVALID_ANIMAL_TYPE_CODE("ANIMAL_002", "유효하지 않은 동물 타입 코드입니다.", HttpStatus.BAD_REQUEST),
	INVALID_NEUTERED_STATE_CODE("ANIMAL_003", "유효하지 않은 중성화 상태 코드입니다.", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}