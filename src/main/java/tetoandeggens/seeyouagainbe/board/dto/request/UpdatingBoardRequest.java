package tetoandeggens.seeyouagainbe.board.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdatingBoardRequest: 실종/동물 게시물 수정 요청 Dto")
public record UpdatingBoardRequest(
	@Schema(description = "제목", example = "[실종] 땡땡이를 찾습니다.")
	String title,

	@Schema(description = "내용", example = "땡땡이를 찾습니다.")
	String content,

	@Schema(description = "축종", example = "DOG")
	String species,

	@Schema(description = "품종", example = "치와와")
	String breedType,

	@Schema(description = "성별", example = "M")
	String sex,

	@Schema(description = "중성화 여부", example = "Y")
	String neuteredState,

	@Schema(description = "색깔", example = "검은색")
	String color,

	@Schema(description = "도로명 주소", example = "서울특별시 ~~")
	String address,

	@Schema(description = "시", example = "서울특별시")
	String city,

	@Schema(description = "구", example = "강남구")
	String town,

	@Schema(description = "위도", example = "12.34")
	Double latitude,

	@Schema(description = "경도", example = "12.34")
	Double longitude,

	@NotBlank(message = "타입은 필수 입니다.")
	@Schema(description = "동물 타입", example = "MISSING")
	String animalType,

	@Max(value = 3, message = "이미지는 최대 3개까지 업로드 가능합니다.")
	@Schema(description = "이미지 개수", example = "2")
	Integer count,

	@Size(max = 10, message = "해시태그는 최대 10개까지 가능합니다.")
	@Schema(description = "해시태그", example = "[\"강아지\", \"실종\"]")
	List<String> tags,

	@Size(max = 3, message = "삭제할 이미지는 최대 3개까지 가능합니다.")
	@Schema(description = "삭제할 이미지 ID 목록", example = "[1, 2, 3]")
	List<Long> deleteImageIds,

	@Size(max = 10, message = "삭제할 해시태그는 최대 10개까지 가능합니다.")
	@Schema(description = "삭제할 해시태그 ID 목록", example = "[1, 2, 3]")
	List<Long> deleteTagIds
) {
}
