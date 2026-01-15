package tetoandeggens.seeyouagainbe.animal.entity;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;

@Getter
@RequiredArgsConstructor
public enum AnimalType {

	ABANDONED("ABANDONED", "유기"),
	MISSING("MISSING", "실종"),
	WITNESS("WITNESS", "목격");

	private final String code;
	private final String type;

	public static AnimalType fromCode(String code) {
		return Arrays.stream(AnimalType.values())
			.filter(animalType -> animalType.code.equals(code))
			.findFirst()
			.orElseThrow(() -> new CustomException(AnimalErrorCode.INVALID_ANIMAL_TYPE_CODE));
	}
}