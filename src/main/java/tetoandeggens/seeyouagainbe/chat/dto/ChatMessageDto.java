package tetoandeggens.seeyouagainbe.chat.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.chat.entity.MessageType;

@Builder
@Schema(name = "ChatMessageDto: 채팅 메시지 Dto")
public record ChatMessageDto(
	@Schema(description = "메시지 타입", example = "TEXT")
	MessageType messageType,

	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "게시물 ID", example = "1")
	Long boardId,

	@Schema(description = "발신자 ID", example = "1")
	Long senderId,

	@Schema(description = "수신자 ID", example = "2")
	Long receiverId,

	@Schema(description = "메시지 내용 (텍스트 메시지인 경우)", example = "이거 얼마에요?")
	String content,

	@Schema(description = "이미지 S3 Key (이미지 메시지인 경우)", example = "chat-images/123/abc-def-123_macbook_photo.jpg")
	String imageKey,

	@Schema(description = "메시지 전송 시간", example = "2025-01-15T14:30:00")
	LocalDateTime time
) {
}