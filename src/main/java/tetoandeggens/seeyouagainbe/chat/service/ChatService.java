package tetoandeggens.seeyouagainbe.chat.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final BoardRepository boardRepository;
	private final MemberRepository memberRepository;

	@Transactional
	public ChatMessageDto saveMessage(ChatMessageDto messageDto) {
		Board board = findBoardWithMember(messageDto.boardId());
		Member sender = findSender(messageDto.senderId());
		Member receiver = board.getMember();

		ChatRoom chatRoom = findOrCreateChatRoom(messageDto, board, sender, receiver);
		ChatMessage savedMessage = saveChatMessage(chatRoom, sender, messageDto);

		chatRoom.updateLastMessageAt(LocalDateTime.now());

		return ChatMessageDto.builder()
			.messageType(savedMessage.getMessageType())
			.chatRoomId(chatRoom.getId())
			.boardId(board.getId())
			.senderId(sender.getId())
			.receiverId(receiver.getId())
			.content(savedMessage.getContent())
			.imageKey(savedMessage.getImageKey())
			.time(savedMessage.getCreatedAt())
			.build();
	}

	private Board findBoardWithMember(Long boardId) {
		Board board = boardRepository.findByIdWithMember(boardId);
		if (board == null) {
			throw new CustomException(ChatErrorCode.BOARD_NOT_FOUND);
		}
		return board;
	}

	private Member findSender(Long senderId) {
		return memberRepository.findById(senderId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.SENDER_NOT_FOUND));
	}

	private ChatRoom findOrCreateChatRoom(ChatMessageDto messageDto, Board board, Member sender, Member receiver) {
		return chatRoomRepository.findByBoardAndMembers(
			messageDto.boardId(),
			messageDto.senderId(),
			messageDto.receiverId()
		).orElseGet(() -> createChatRoom(board, sender, receiver));
	}

	private ChatRoom createChatRoom(Board board, Member sender, Member receiver) {
		ChatRoom newChatRoom = ChatRoom.builder()
			.board(board)
			.sender(sender)
			.receiver(receiver)
			.contentType(board.getContentType())
			.violatedStatus(ViolatedStatus.NORMAL)
			.build();
		return chatRoomRepository.save(newChatRoom);
	}

	private ChatMessage saveChatMessage(ChatRoom chatRoom, Member sender, ChatMessageDto messageDto) {
		ChatMessage chatMessage = ChatMessage.builder()
			.chatRoom(chatRoom)
			.sender(sender)
			.messageType(messageDto.messageType())
			.content(messageDto.content())
			.imageKey(messageDto.imageKey())
			.build();
		return chatMessageRepository.save(chatMessage);
	}
}
