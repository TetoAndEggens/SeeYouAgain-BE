package tetoandeggens.seeyouagainbe.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ChatMessageSendRequest", description = "채팅 메시지 전송 요청 Dto")
public record ChatMessageSendRequest(
	@Schema(description = "채팅방 ID", example = "1")
	@NotNull(message = "채팅방 ID는 필수입니다")
	Long chatRoomId,

	@Schema(description = "메시지 내용", example = "이거 얼마에요?")
	@NotBlank(message = "메시지 내용은 필수입니다")
	String content
) {
}