package tetoandeggens.seeyouagainbe.chat.dto.response;

import lombok.Builder;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;

@Builder
public record ChatMessageResponse(
	Long messageId,
	Long senderId,
	String content,
	Boolean isRead,
	Boolean isMyChat,
	String createdAt
) {
	public static ChatMessageResponse from(ChatMessage message, Long currentMemberId) {
		return ChatMessageResponse.builder()
			.messageId(message.getId())
			.senderId(message.getSender().getId())
			.content(message.getContent())
			.isRead(message.getIsRead())
			.isMyChat(message.getSender().getId().equals(currentMemberId))
			.createdAt(message.getCreatedAt().toString())
			.build();
	}
}