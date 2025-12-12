package tetoandeggens.seeyouagainbe.chat.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "ChatMessageDto", description = "채팅 메시지 Dto")
public record ChatMessageDto(
	@Schema(description = "채팅방 ID", example = "1")
	Long chatRoomId,

	@Schema(description = "게시물 ID", example = "1")
	Long boardId,

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