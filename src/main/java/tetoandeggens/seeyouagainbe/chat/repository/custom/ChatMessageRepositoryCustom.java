package tetoandeggens.seeyouagainbe.chat.repository.custom;

import java.util.List;
import java.util.Optional;

import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

public interface ChatMessageRepositoryCustom {

	List<ChatMessage> findMessagesByChatRoom(Long chatRoomId, Long cursorId, int size, SortDirection sortDirection);

	void markAsReadByChatRoomAndReceiver(Long chatRoomId, Long memberId);

	Optional<ChatMessage> findByIdWithChatRoomAndMembersAndValidateAccess(Long messageId, Long memberId);
}