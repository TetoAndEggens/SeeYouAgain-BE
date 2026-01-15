package tetoandeggens.seeyouagainbe.common.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BoardErrorCode;

@Getter
@RequiredArgsConstructor
public enum ContentType {
	WITNESS("목격"),
	MISSING("실종");

	private final String type;

	public static ContentType fromCode(String code) {
		return Arrays.stream(ContentType.values())
			.filter(contentType -> contentType.name().equalsIgnoreCase(code))
			.findFirst()
			.orElseThrow(() -> new CustomException(BoardErrorCode.INVALID_CONTENT_TYPE));
	}
}