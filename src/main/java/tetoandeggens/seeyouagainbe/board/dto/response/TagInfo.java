package tetoandeggens.seeyouagainbe.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TagInfo(
	@Schema(description = "해시태그 id", example = "1")
	Long tagId,
	@Schema(description = "해시태그", example = "[\"강아지\"]")
	String tag
) {
}
