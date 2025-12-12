package tetoandeggens.seeyouagainbe.animal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;

@Schema(name = "AnimalListResponse", description = "유기 동물 리스트 응답 Dto")
public record AnimalListResponse(
	@Schema(description = "총 유기 동물 수", example = "24")
	int animalCount,

	CursorPage<AnimalResponse, Long> animal
) {
	public static AnimalListResponse of(int animalCount,
		CursorPage<AnimalResponse, Long> animal) {
		return new AnimalListResponse(animalCount, animal);
	}
}