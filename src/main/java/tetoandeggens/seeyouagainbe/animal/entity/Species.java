package tetoandeggens.seeyouagainbe.animal.entity;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Species {
	DOG("417000", "개"),
	CAT("422400", "고양이"),
	ETC("429900", "기타");

	private final String code;
	private final String type;

	public static Species fromCode(String code) {
		return Arrays.stream(Species.values())
			.filter(species -> species.code.equals(code))
			.findFirst()
			.orElse(ETC);
	}
}