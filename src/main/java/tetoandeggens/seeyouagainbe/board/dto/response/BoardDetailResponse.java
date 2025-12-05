package tetoandeggens.seeyouagainbe.board.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;

@Schema(name = "BoardDetailResponse: 게시글 상세 응답 Dto")
public record BoardDetailResponse(
	@NotBlank(message = "게시글 고유 id입니다.")
	@Schema(description = "게시글 고유 id", example = "1")
	Long boardId,

	@Schema(description = "제목", example = "[실종] 땡땡이를 찾습니다.")
	String title,

	@Schema(description = "내용", example = "땡땡이를 찾습니다.")
	String content,

	@Schema(description = "종", example = "DOG")
	Species species,

	@Schema(description = "품종", example = "치와와")
	String breedType,

	@Schema(description = "성별", example = "M")
	Sex sex,

	@Schema(description = "중성화 여부", example = "Y")
	NeuteredState neuteredState,

	@Schema(description = "색상", example = "흰색")
	String color,

	@Schema(description = "주소", example = "서울특별시 강남구")
	String address,

	@Schema(description = "시", example = "서울특별시")
	String city,

	@Schema(description = "구", example = "강남구")
	String town,

	@Schema(description = "위도", example = "37.4979")
	Double latitude,

	@Schema(description = "경도", example = "127.0276")
	Double longitude,

	@Schema(description = "동물 타입", example = "MISSING")
	AnimalType animalType,

	@Schema(description = "작성자 닉네임", example = "홍길동")
	String memberNickname,

	@Schema(description = "생성일시", example = "2024-01-01T00:00:00")
	LocalDateTime createdAt,

	@Schema(description = "수정일시", example = "2024-01-01T00:00:00")
	LocalDateTime updatedAt,

	@Schema(description = "해시태그", example = "[\"강아지\", \"실종\"]")
	List<TagInfo> tags,

	@Schema(description = "프로필 url 리스트 (최대 3개)", example = "[\"https://프로필1.com\", \"https://프로필2.com\", \"https://프로필3.com\"]")
	List<ProfileInfo> profiles,

	@Schema(description = "북마크 여부", example = "true")
	Boolean isBookmarked
) {
}
