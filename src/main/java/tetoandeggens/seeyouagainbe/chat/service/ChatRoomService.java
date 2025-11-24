package tetoandeggens.seeyouagainbe.chat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional(readOnly = true)
	public CursorPage<ChatRoomResponse, Long> getMyChatRooms(Long userId, CursorPageRequest request,
		SortDirection sortDirection) {
		List<ChatRoomResponse> chatRooms = chatRoomRepository.findChatRoomsWithDetails(
			userId,
			request.cursorId(),
			request.size(),
			sortDirection
		);

		return CursorPage.of(chatRooms, request.size(), ChatRoomResponse::chatRoomId);
	}

	@Transactional(readOnly = true)
	public CursorPage<ChatRoomResponse, Long> getUnreadChatRooms(Long userId, CursorPageRequest request,
		SortDirection sortDirection) {
		List<ChatRoomResponse> chatRooms = chatRoomRepository.findUnreadChatRoomsWithDetails(
			userId,
			request.cursorId(),
			request.size(),
			sortDirection
		);

		return CursorPage.of(chatRooms, request.size(), ChatRoomResponse::chatRoomId);
	}

	@Transactional
	public CursorPage<ChatMessageResponse, Long> getChatMessages(Long chatRoomId, Long userId,
		CursorPageRequest request, SortDirection sortDirection) {
		ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(chatRoomId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

		boolean isChatRoomMember =
			chatRoom.getSender().getId().equals(userId) || chatRoom.getReceiver().getId().equals(userId);

		if (!isChatRoomMember) {
			throw new CustomException(ChatErrorCode.CHAT_FORBIDDEN);
		}

		chatMessageRepository.markAsReadByChatRoomAndReceiver(chatRoomId, userId);

		List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
			chatRoomId,
			request.cursorId(),
			request.size(),
			sortDirection
		);

		List<ChatMessageResponse> responses = new ArrayList<>();
		for (ChatMessage message : messages) {
			responses.add(ChatMessageResponse.from(message));
		}

		return CursorPage.of(responses, request.size(), ChatMessageResponse::messageId);
	}
}