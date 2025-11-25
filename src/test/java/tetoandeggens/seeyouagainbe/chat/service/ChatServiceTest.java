package tetoandeggens.seeyouagainbe.chat.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("ChatService 통합 테스트")
class ChatServiceTest extends ServiceTest {

	@Autowired
	private ChatService chatService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Board testBoard;
	private Member sender;
	private Member receiver;

	@BeforeEach
	void setUp() {
		chatMessageRepository.deleteAll();
		chatRoomRepository.deleteAll();
		boardRepository.deleteAll();
		memberRepository.deleteAll();

		sender = memberRepository.save(Member.builder()
			.loginId("sender123")
			.password("password")
			.nickName("발신자")
			.phoneNumber("01012345678")
			.build());

		receiver = memberRepository.save(Member.builder()
			.loginId("receiver123")
			.password("password")
			.nickName("수신자")
			.phoneNumber("01087654321")
			.build());

		testBoard = boardRepository.save(Board.builder()
			.member(sender)
			.title("테스트 게시글")
			.content("테스트 내용")
			.contentType(ContentType.MISSING)
			.build());
	}

	@Nested
	@DisplayName("메시지 저장 테스트")
	class SaveMessageTests {

		@Test
		@DisplayName("새 채팅방 생성하며 메시지 저장 - 성공")
		void saveMessage_WithNewChatRoom_Success() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(testBoard.getId())
				.senderId(sender.getId())
				.receiverId(receiver.getId())
				.content("첫 메시지")
				.build();

			// when
			ChatMessageDto result = chatService.saveMessage(messageDto);

			// then
			assertThat(result).isNotNull();
			assertThat(result.boardId()).isEqualTo(testBoard.getId());
			assertThat(result.senderId()).isEqualTo(sender.getId());
			assertThat(result.receiverId()).isEqualTo(receiver.getId());
			assertThat(result.content()).isEqualTo("첫 메시지");

			// 채팅방과 메시지가 실제로 저장되었는지 확인
			assertThat(chatRoomRepository.count()).isEqualTo(1);
			assertThat(chatMessageRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("기존 채팅방이 있을 때 메시지 저장 - 성공")
		void saveMessage_WithExistingChatRoom_Success() {
			// given - 첫 번째 메시지로 채팅방 생성
			ChatMessageDto firstMessage = ChatMessageDto.builder()
				.boardId(testBoard.getId())
				.senderId(sender.getId())
				.receiverId(receiver.getId())
				.content("첫 메시지")
				.build();
			chatService.saveMessage(firstMessage);

			// given - 두 번째 메시지
			ChatMessageDto secondMessage = ChatMessageDto.builder()
				.boardId(testBoard.getId())
				.senderId(sender.getId())
				.receiverId(receiver.getId())
				.content("두 번째 메시지")
				.build();

			// when
			ChatMessageDto result = chatService.saveMessage(secondMessage);

			// then
			assertThat(result).isNotNull();
			assertThat(result.content()).isEqualTo("두 번째 메시지");

			// 채팅방은 하나만 존재하고 메시지는 2개
			assertThat(chatRoomRepository.count()).isEqualTo(1);
			assertThat(chatMessageRepository.count()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("메시지 저장 실패 테스트")
	class SaveMessageFailureTests {

		@Test
		@DisplayName("존재하지 않는 게시물 - BOARD_NOT_FOUND 예외 발생")
		void saveMessage_BoardNotFound_ThrowsException() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(999L)
				.senderId(sender.getId())
				.receiverId(receiver.getId())
				.content("메시지")
				.build();

			// when & then
			assertThatThrownBy(() -> chatService.saveMessage(messageDto))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.BOARD_NOT_FOUND);
		}

		@Test
		@DisplayName("존재하지 않는 발신자 - SENDER_NOT_FOUND 예외 발생")
		void saveMessage_SenderNotFound_ThrowsException() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(testBoard.getId())
				.senderId(999L)
				.receiverId(receiver.getId())
				.content("메시지")
				.build();

			// when & then
			assertThatThrownBy(() -> chatService.saveMessage(messageDto))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.SENDER_NOT_FOUND);
		}

		@Test
		@DisplayName("존재하지 않는 수신자 - RECEIVER_NOT_FOUND 예외 발생")
		void saveMessage_ReceiverNotFound_ThrowsException() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(testBoard.getId())
				.senderId(sender.getId())
				.receiverId(999L)
				.content("메시지")
				.build();

			// when & then
			assertThatThrownBy(() -> chatService.saveMessage(messageDto))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.RECEIVER_NOT_FOUND);
		}
	}
}