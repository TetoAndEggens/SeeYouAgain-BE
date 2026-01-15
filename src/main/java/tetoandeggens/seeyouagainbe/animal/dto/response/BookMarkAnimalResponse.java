package tetoandeggens.seeyouagainbe.animal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.animal.entity.Species;

@Builder
@Schema(name = "BookMarkAnimalResponse", description = "북마크한 동물 정보 응답 Dto")
public record BookMarkAnimalResponse(
        @Schema(description = "북마크 고유 ID", example = "1")
        Long bookMarkId,

        @Schema(description = "동물 고유 ID", example = "123")
        Long animalId,

        @Schema(description = "동물 종", example = "DOG")
        Species species,

        @Schema(description = "품종", example = "치와와")
        String breedType,

        @Schema(description = "보호 상태", example = "보호중")
        String processState
) {}