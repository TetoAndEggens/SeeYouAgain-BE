package tetoandeggens.seeyouagainbe.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;

@Schema(name = "ChatRoomListResponse: 채팅방 리스트 응답 Dto")
public record ChatRoomListResponse(
	CursorPage<ChatRoomResponse, Long> chatRooms
) {
	public static ChatRoomListResponse of(CursorPage<ChatRoomResponse, Long> chatRooms) {
		return new ChatRoomListResponse(chatRooms);
	}
}