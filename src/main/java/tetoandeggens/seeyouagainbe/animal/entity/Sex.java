package tetoandeggens.seeyouagainbe.animal.entity;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Sex {
	M("M", "수컷"),
	F("F", "암컷"),
	Q("Q", "미상");

	private final String code;
	private final String type;

	public static Sex fromCode(String code) {
		return Arrays.stream(Sex.values())
			.filter(sex -> sex.code.equals(code))
			.findFirst()
			.orElse(Q);
	}
}