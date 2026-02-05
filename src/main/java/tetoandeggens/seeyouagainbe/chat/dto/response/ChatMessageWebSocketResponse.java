package tetoandeggens.seeyouagainbe.chat.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;

@Builder
@Schema(name = "ChatMessageWebSocketResponse", description = "채팅 메시지 WebSocket 응답 DTO")
public record ChatMessageWebSocketResponse(
	@Schema(description = "메시지 ID", example = "1")
	Long messageId,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "발신자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
	String senderUuid,

	@Schema(description = "메시지 내용", example = "이거 얼마에요?")
	String content,

	@Schema(description = "내가 보낸 메시지인지 여부", example = "true")
	Boolean isMyChat,

	@Schema(description = "메시지 전송 시간", example = "2025-01-15T14:30:00")
	LocalDateTime time
) {
	public static ChatMessageWebSocketResponse from(ChatMessageDto dto, Long currentMemberId) {
		return ChatMessageWebSocketResponse.builder()
			.messageId(dto.messageId())
			.chatRoomId(dto.chatRoomId())
			.senderUuid(dto.senderUuid())
			.content(dto.content())
			.isMyChat(dto.senderId().equals(currentMemberId))
			.time(dto.time())
			.build();
	}
}