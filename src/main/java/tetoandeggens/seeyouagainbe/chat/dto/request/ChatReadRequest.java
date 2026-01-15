package tetoandeggens.seeyouagainbe.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ChatReadDto", description = "채팅 읽음 처리 요청 Dto")
public record ChatReadRequest(
	@Schema(description = "메시지 ID", example = "1")
	Long messageId
) {
}
