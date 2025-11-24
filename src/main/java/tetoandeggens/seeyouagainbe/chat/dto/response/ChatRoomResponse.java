package tetoandeggens.seeyouagainbe.chat.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;

@Builder
public record ChatRoomResponse(
	Long chatRoomId,
	Long boardId,
	String boardTitle,
	ContentType contentType,
	Long senderId,
	Long receiverId,
	String otherMemberNickname,
	String lastMessage,
	LocalDateTime lastMessageTime,
	long unreadCount
) {
}