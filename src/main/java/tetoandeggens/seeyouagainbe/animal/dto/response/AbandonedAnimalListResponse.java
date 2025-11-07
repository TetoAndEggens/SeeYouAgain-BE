package tetoandeggens.seeyouagainbe.animal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;

@Schema(name = "AbandonedAnimalListResponse: 유기 동물 리스트 응답 Dto")
public record AbandonedAnimalListResponse(
	@Schema(description = "총 유기 동물 수", example = "24")
	int abandonedAnimalCount,

	CursorPage<AbandonedAnimalResponse, Long> abandonedAnimal
) {
	public static AbandonedAnimalListResponse of(int abandonedAnimalCount,
		CursorPage<AbandonedAnimalResponse, Long> abandonedAnimal) {
		return new AbandonedAnimalListResponse(abandonedAnimalCount, abandonedAnimal);
	}
}