package tetoandeggens.seeyouagainbe.chat.repository.custom;

import java.util.List;
import java.util.Optional;

import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

public interface ChatRoomRepositoryCustom {

	Optional<Long> findChatRoomIdByBoardAndMembers(Long boardId, Long senderId, Long receiverId);

	Optional<ChatRoom> findByIdWithMembers(Long chatRoomId);

	Optional<ChatRoom> findByIdWithMembersAndValidateAccess(Long chatRoomId, Long memberId);

	List<ChatRoomResponse> findChatRoomsWithDetails(Long memberId, Long cursorId, int size, SortDirection sortDirection);

	List<ChatRoomResponse> findUnreadChatRoomsWithDetails(Long memberId, Long cursorId, int size, SortDirection sortDirection);
}