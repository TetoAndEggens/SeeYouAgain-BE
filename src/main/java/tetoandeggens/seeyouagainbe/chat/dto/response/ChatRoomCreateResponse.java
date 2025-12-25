package tetoandeggens.seeyouagainbe.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "ChatRoomCreateResponse", description = "채팅방 생성/조회 응답 Dto")
public record ChatRoomCreateResponse(
	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId
) {
}