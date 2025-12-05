package tetoandeggens.seeyouagainbe.animal.entity;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;

@Getter
@RequiredArgsConstructor
public enum NeuteredState {
	Y("Y", "중성화"),
	N("N", "비중성화"),
	U("U", "미상");

	private final String code;
	private final String type;

	public static NeuteredState fromCode(String code) {
		return Arrays.stream(NeuteredState.values())
			.filter(neuteredState -> neuteredState.code.equals(code))
			.findFirst()
			.orElseThrow(() -> new CustomException(AnimalErrorCode.INVALID_NEUTERED_STATE_CODE));
	}
}