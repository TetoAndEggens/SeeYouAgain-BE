package tetoandeggens.seeyouagainbe.chat.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "ChatMessageDto", description = "채팅 메시지 Dto (WebSocket 응답 및 Redis pub/sub용)")
public record ChatMessageDto(
	@Schema(description = "메시지 ID", example = "1")
	Long messageId,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "발신자 ID", example = "1")
	Long senderId,

	@Schema(description = "수신자 ID", example = "2")
	Long receiverId,

	@Schema(description = "메시지 내용", example = "이거 얼마에요?")
	String content,

	@Schema(description = "메시지 전송 시간", example = "2025-01-15T14:30:00")
	LocalDateTime time
) {
}