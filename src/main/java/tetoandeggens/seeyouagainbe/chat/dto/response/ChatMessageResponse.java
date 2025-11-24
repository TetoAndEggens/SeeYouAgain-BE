package tetoandeggens.seeyouagainbe.chat.dto.response;

import lombok.Builder;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;

@Builder
public record ChatMessageResponse(
	Long messageId,
	Long senderId,
	String messageType,
	String content,
	String imageKey,
	Boolean isRead,
	String createdAt
) {
	public static ChatMessageResponse from(ChatMessage message) {
		return ChatMessageResponse.builder()
			.messageId(message.getId())
			.senderId(message.getSender().getId())
			.messageType(message.getMessageType().name())
			.content(message.getContent())
			.imageKey(message.getImageKey())
			.isRead(message.getIsRead())
			.createdAt(message.getCreatedAt().toString())
			.build();
	}
}