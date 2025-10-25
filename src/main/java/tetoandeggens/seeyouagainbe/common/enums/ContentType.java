package tetoandeggens.seeyouagainbe.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {
	WITNESS("목격"),
	MISSING("실종");

	private final String type;
}