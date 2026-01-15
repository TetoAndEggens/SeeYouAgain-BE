package tetoandeggens.seeyouagainbe.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileInfo(
	@Schema(description = "프로필 id", example = "1")
	Long profileId,
	@Schema(description = "프로필", example = "https://프로필1.com")
	String profile
) {
}
