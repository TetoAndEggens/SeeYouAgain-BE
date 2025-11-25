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
		Board board = findBoard(messageDto.boardId());
		Member sender = findMember(messageDto.senderId(), ChatErrorCode.SENDER_NOT_FOUND);
		Member receiver = findMember(messageDto.receiverId(), ChatErrorCode.RECEIVER_NOT_FOUND);

		ChatRoom chatRoom = findOrCreateChatRoom(messageDto, board, sender, receiver);
		ChatMessage savedMessage = saveChatMessage(chatRoom, sender, messageDto);

		chatRoom.updateLastMessageAt(LocalDateTime.now());

		return ChatMessageDto.builder()
			.chatRoomId(chatRoom.getId())
			.boardId(board.getId())
			.senderId(sender.getId())
			.receiverId(receiver.getId())
			.content(savedMessage.getContent())
			.time(savedMessage.getCreatedAt())
			.build();
	}

	private Board findBoard(Long boardId) {
		return boardRepository.findById(boardId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.BOARD_NOT_FOUND));
	}

	private Member findMember(Long memberId, ChatErrorCode errorCode) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(errorCode));
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
			.content(messageDto.content())
			.build();
		return chatMessageRepository.save(chatMessage);
	}
}
