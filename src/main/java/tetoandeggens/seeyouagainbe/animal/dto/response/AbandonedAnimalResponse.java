package tetoandeggens.seeyouagainbe.animal.dto.response;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;

@Builder
@Schema(name = "AbandonedAnimalResponse: 유기 동물 정보 응답 리스트 Dto")
public record AbandonedAnimalResponse(
	@NotBlank(message = "유기 동물 고유 id입니다.")
	@Schema(description = "유기 동물 고유 id", example = "1")
	Long abandonedAnimalId,

	@Schema(description = "발생 날짜", example = "20250101")
	LocalDate happenDate,

	@Schema(description = "종", example = "DOG")
	Species species,

	@Schema(description = "품종", example = "치와와")
	String breedType,

	@Schema(description = "태어난 연도", example = "2025년생")
	String birth,

	@Schema(description = "시/도", example = "서울특별시")
	String city,

	@Schema(description = "군/구", example = "강남구")
	String town,

	@Schema(description = "성별", example = "M")
	Sex sex,

	@Schema(description = "상태", example = "보호중")
	String processState,

	@Schema(description = "프로필 url", example = "https://프로필.com")
	String profile
) {
}
