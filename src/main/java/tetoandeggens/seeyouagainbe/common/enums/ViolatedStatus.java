package tetoandeggens.seeyouagainbe.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ViolatedStatus {
	WAITING("대기"),
	VIOLATED("위반"),
	NORMAL("정상");

	private final String type;
}