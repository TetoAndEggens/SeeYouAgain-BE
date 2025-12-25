package tetoandeggens.seeyouagainbe.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ChatRoomCreateRequest", description = "채팅방 생성 요청 Dto")
public record ChatRoomCreateRequest(
	@Schema(description = "게시글 ID", example = "1")
	@NotNull(message = "게시글 ID는 필수입니다")
	Long boardId
) {
}