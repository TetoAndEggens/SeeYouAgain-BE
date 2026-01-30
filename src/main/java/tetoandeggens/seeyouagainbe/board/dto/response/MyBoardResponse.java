package tetoandeggens.seeyouagainbe.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(name = "MyBoardResponse", description = "내가 작성한 게시글 정보 응답 DTO")
public record MyBoardResponse(
    @Schema(description = "게시글 고유 id", example = "1")
    Long boardId,

    @Schema(description = "동물 타입", example = "MISSING")
    AnimalType animalType,

    @Schema(description = "제목", example = "[실종] 땡땡이를 찾습니다.")
    String title,

    @Schema(description = "주소", example = "서울특별시 강남구")
    String address,

    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    LocalDateTime updatedAt,

    @Schema(description = "해시태그", example = "[\"강아지\", \"실종\"]")
    List<String> tags,

    @Schema(description = "첫번째 프로필 url (있는 경우)", example = "https://프로필.com")
    String profile
) {
}