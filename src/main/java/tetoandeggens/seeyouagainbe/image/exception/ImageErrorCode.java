package tetoandeggens.seeyouagainbe.image.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

	INVALID_IMAGE_TYPE("IMAGE_001", "지원하지 않는 이미지 타입입니다. (허용: jpg, jpeg, png, webp)", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}