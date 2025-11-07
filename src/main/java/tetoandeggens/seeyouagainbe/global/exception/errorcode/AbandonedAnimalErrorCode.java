package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalErrorCode implements ErrorCode {

	ABANDONED_ANIMAL_NOT_FOUND("ANIMAL_001", "유기 동물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}