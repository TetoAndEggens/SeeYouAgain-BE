package tetoandeggens.seeyouagainbe.chat.repository.custom;

import java.util.List;
import java.util.Optional;

import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

public interface ChatRoomRepositoryCustom {

	Optional<ChatRoom> findByBoardAndMembers(Long boardId, Long senderId, Long receiverId);

	Optional<ChatRoom> findByIdWithMembers(Long chatRoomId);

	List<ChatRoomResponse> findChatRoomsWithDetails(Long userId, Long cursorId, int size, SortDirection sortDirection);

	List<ChatRoomResponse> findUnreadChatRoomsWithDetails(Long userId, Long cursorId, int size, SortDirection sortDirection);
}