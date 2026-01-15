package tetoandeggens.seeyouagainbe.chat.dto.response;

import lombok.Builder;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;

@Builder
public record ChatMessageResponse(
	Long messageId,
	Long senderId,
	String content,
	Boolean isRead,
	String createdAt
) {
	public static ChatMessageResponse from(ChatMessage message) {
		return ChatMessageResponse.builder()
			.messageId(message.getId())
			.senderId(message.getSender().getId())
			.content(message.getContent())
			.isRead(message.getIsRead())
			.createdAt(message.getCreatedAt().toString())
			.build();
	}
}