package tetoandeggens.seeyouagainbe.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;

@Schema(name = "ChatMessageListResponse: 채팅 메시지 리스트 응답 Dto")
public record ChatMessageListResponse(
	CursorPage<ChatMessageResponse, Long> messages
) {
	public static ChatMessageListResponse of(CursorPage<ChatMessageResponse, Long> messages) {
		return new ChatMessageListResponse(messages);
	}
}