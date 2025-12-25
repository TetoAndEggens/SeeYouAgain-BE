package tetoandeggens.seeyouagainbe.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "ChatReadNotification", description = "채팅 읽음 알림 Dto")
public record ChatReadNotificationDto(
	@Schema(description = "메시지 ID", example = "1")
	Long messageId,

	@Schema(description = "발신자 ID", example = "1")
	Long senderId
) {
}
