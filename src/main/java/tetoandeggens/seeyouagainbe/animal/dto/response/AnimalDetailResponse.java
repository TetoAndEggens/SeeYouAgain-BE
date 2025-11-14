package tetoandeggens.seeyouagainbe.animal.dto.response;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;

@Builder
@Schema(name = "AnimalDetailResponse: 유기 동물 상세 정보 응답 Dto")
public record AnimalDetailResponse(
	@NotBlank(message = "유기 동물 고유 id입니다.")
	@Schema(description = "유기 동물 고유 id", example = "1")
	Long animalId,

	@Schema(description = "발생 날짜", example = "20250101")
	LocalDate happenDate,

	@Schema(description = "종", example = "DOG")
	Species species,

	@Schema(description = "품종", example = "치와와")
	String breedType,

	@Schema(description = "태어난 연도", example = "2025년생")
	String birth,

	@Schema(description = "발견 장소", example = "서울특별시 어쩌구 저쩌구")
	String happenPlace,

	@Schema(description = "성별", example = "M")
	Sex sex,

	@Schema(description = "상태", example = "보호중")
	String processState,

	@Schema(description = "프로필 url 리스트 (최대 3개)", example = "[\"https://프로필1.com\", \"https://프로필2.com\", \"https://프로필3.com\"]")
	List<String> profiles,

	@Schema(description = "색깔", example = "흰색")
	String color,

	@Schema(description = "공고번호", example = "경북-경주-2025-01056")
	String noticeNo,

	@Schema(description = "공고 시작일", example = "2025-01-01")
	String noticeStartDate,

	@Schema(description = "공고 종료일", example = "2025-12-31")
	String noticeEndDate,

	@Schema(description = "특징", example = "귀엽다")
	String specialMark,

	@Schema(description = "무게", example = "3.3(Kg)")
	String weight,

	@Schema(description = "중성화 여부", example = "Y")
	NeuteredState neuteredState,

	@Schema(description = "보호소명", example = "유기견 보호소")
	String centerName,

	@Schema(description = "보호소 위치", example = "서울특별시 어쩌구")
	String centerAddress,

	@Schema(description = "보호소 전화번호", example = "02-1234-5678")
	String centerPhone
) {
}