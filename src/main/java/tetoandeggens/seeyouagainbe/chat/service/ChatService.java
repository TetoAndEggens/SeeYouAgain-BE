package tetoandeggens.seeyouagainbe.chat.service;

import java.security.Principal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.dto.ChatReadNotificationDto;
import tetoandeggens.seeyouagainbe.chat.dto.request.ChatMessageSendRequest;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomCreateResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.pub.RedisPublisher;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final BoardRepository boardRepository;
	private final RedisPublisher redisPublisher;

	@Transactional
	public ChatRoomCreateResponse findOrCreateChatRoom(Long boardId, Long memberId) {
		Board board = boardRepository.findByIdWithMember(boardId);
		if (board == null) {
			throw new CustomException(ChatErrorCode.BOARD_NOT_FOUND);
		}

		Member author = board.getMember();

		if (author.getId().equals(memberId)) {
			throw new CustomException(ChatErrorCode.CANNOT_CHAT_WITH_SELF);
		}

		Long chatRoomId = chatRoomRepository.findChatRoomIdByBoardAndMembers(boardId, memberId, author.getId())
			.orElseGet(() -> createChatRoom(board, new Member(memberId), author));

		return ChatRoomCreateResponse.builder()
			.chatRoomId(chatRoomId)
			.build();
	}

	@Transactional
	public void sendMessage(ChatMessageSendRequest request, Principal principal) {
		if (principal == null) {
			throw new CustomException(AuthErrorCode.INVALID_TOKEN);
		}

		Long authenticatedMemberId = Long.parseLong(principal.getName());

		ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(request.chatRoomId())
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

		Long receiverId = determineReceiverId(chatRoom, authenticatedMemberId);

		ChatMessageDto savedMessage = saveMessage(
			request.chatRoomId(),
			authenticatedMemberId,
			receiverId,
			request.content()
		);

		redisPublisher.publish(savedMessage);
	}

	private Long determineReceiverId(ChatRoom chatRoom, Long senderId) {
		if (chatRoom.getSender().getId().equals(senderId)) {
			return chatRoom.getReceiver().getId();
		} else if (chatRoom.getReceiver().getId().equals(senderId)) {
			return chatRoom.getSender().getId();
		} else {
			throw new CustomException(ChatErrorCode.CHAT_FORBIDDEN);
		}
	}

	private ChatMessageDto saveMessage(Long chatRoomId, Long senderId, Long receiverId, String content) {
		ChatMessage savedMessage = ChatMessage.builder()
			.chatRoom(new ChatRoom(chatRoomId))
			.sender(new Member(senderId))
			.receiver(new Member(receiverId))
			.content(content)
			.build();

		savedMessage = chatMessageRepository.save(savedMessage);

		return ChatMessageDto.builder()
			.messageId(savedMessage.getId())
			.chatRoomId(chatRoomId)
			.senderId(senderId)
			.receiverId(receiverId)
			.content(content)
			.time(savedMessage.getCreatedAt())
			.build();
	}

	@Transactional
	public void markAsRead(Long messageId, Principal principal) {
		if (principal == null) {
			throw new CustomException(AuthErrorCode.INVALID_TOKEN);
		}

		Long readerId = Long.parseLong(principal.getName());

		ChatMessage message = chatMessageRepository.findByIdWithMembers(messageId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.MESSAGE_NOT_FOUND));

		if (!message.getReceiver().getId().equals(readerId)) {
			throw new CustomException(ChatErrorCode.NOT_MESSAGE_RECEIVER);
		}

		message.markAsRead();

		ChatReadNotificationDto notification = ChatReadNotificationDto.builder()
			.messageId(message.getId())
			.senderId(message.getSender().getId())
			.build();

		redisPublisher.publishReadNotification(notification);
	}

	private Long createChatRoom(Board board, Member sender, Member receiver) {
		ChatRoom newChatRoom = ChatRoom.builder()
			.board(board)
			.sender(sender)
			.receiver(receiver)
			.violatedStatus(ViolatedStatus.NORMAL)
			.build();
		chatRoomRepository.save(newChatRoom);

		return newChatRoom.getId();
	}
}
