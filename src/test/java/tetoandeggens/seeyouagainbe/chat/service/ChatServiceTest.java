package tetoandeggens.seeyouagainbe.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.dto.ChatMessageDto;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

	@InjectMocks
	private ChatService chatService;

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private BoardRepository boardRepository;

	@Mock
	private MemberRepository memberRepository;

	private Board testBoard;
	private Member sender;
	private Member receiver;
	private ChatRoom chatRoom;
	private ChatMessage chatMessage;

	@BeforeEach
	void setUp() {
		testBoard = mock(Board.class);
		lenient().when(testBoard.getId()).thenReturn(1L);
		lenient().when(testBoard.getContentType()).thenReturn(ContentType.MISSING);

		sender = mock(Member.class);
		lenient().when(sender.getId()).thenReturn(1L);
		lenient().when(sender.getLoginId()).thenReturn("sender123");
		lenient().when(sender.getNickName()).thenReturn("발신자");

		receiver = mock(Member.class);
		lenient().when(receiver.getId()).thenReturn(2L);
		lenient().when(receiver.getLoginId()).thenReturn("receiver123");
		lenient().when(receiver.getNickName()).thenReturn("수신자");

		chatRoom = mock(ChatRoom.class);
		lenient().when(chatRoom.getId()).thenReturn(1L);
		lenient().when(chatRoom.getBoard()).thenReturn(testBoard);
		lenient().when(chatRoom.getSender()).thenReturn(sender);
		lenient().when(chatRoom.getReceiver()).thenReturn(receiver);

		chatMessage = mock(ChatMessage.class);
		lenient().when(chatMessage.getChatRoom()).thenReturn(chatRoom);
		lenient().when(chatMessage.getSender()).thenReturn(sender);
		lenient().when(chatMessage.getContent()).thenReturn("테스트 메시지");
	}

	@Nested
	@DisplayName("메시지 저장 테스트")
	class SaveMessageTests {

		@Test
		@DisplayName("기존 채팅방이 있을 때 메시지 저장 - 성공")
		void saveMessage_WithExistingChatRoom_Success() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(1L)
				.senderId(1L)
				.receiverId(2L)
				.content("안녕하세요")
				.build();

			given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));
			given(memberRepository.findById(1L)).willReturn(Optional.of(sender));
			given(memberRepository.findById(2L)).willReturn(Optional.of(receiver));
			given(chatRoomRepository.findByBoardAndMembers(1L, 1L, 2L))
				.willReturn(Optional.of(chatRoom));
			given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

			// when
			ChatMessageDto result = chatService.saveMessage(messageDto);

			// then
			assertThat(result).isNotNull();
			assertThat(result.boardId()).isEqualTo(1L);
			assertThat(result.senderId()).isEqualTo(1L);
			assertThat(result.receiverId()).isEqualTo(2L);
			assertThat(result.content()).isEqualTo("테스트 메시지");

			verify(boardRepository).findById(1L);
			verify(memberRepository).findById(1L);
			verify(memberRepository).findById(2L);
			verify(chatRoomRepository).findByBoardAndMembers(1L, 1L, 2L);
			verify(chatMessageRepository).save(any(ChatMessage.class));
			verify(chatRoomRepository, never()).save(any(ChatRoom.class));
		}

		@Test
		@DisplayName("새 채팅방 생성하며 메시지 저장 - 성공")
		void saveMessage_WithNewChatRoom_Success() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(1L)
				.senderId(1L)
				.receiverId(2L)
				.content("첫 메시지")
				.build();

			given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));
			given(memberRepository.findById(1L)).willReturn(Optional.of(sender));
			given(memberRepository.findById(2L)).willReturn(Optional.of(receiver));
			given(chatRoomRepository.findByBoardAndMembers(1L, 1L, 2L))
				.willReturn(Optional.empty());
			given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
			given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

			// when
			ChatMessageDto result = chatService.saveMessage(messageDto);

			// then
			assertThat(result).isNotNull();
			assertThat(result.boardId()).isEqualTo(1L);

			verify(chatRoomRepository).findByBoardAndMembers(1L, 1L, 2L);
			verify(chatRoomRepository).save(any(ChatRoom.class));
			verify(chatMessageRepository).save(any(ChatMessage.class));
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
				.senderId(1L)
				.receiverId(2L)
				.content("메시지")
				.build();

			given(boardRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatService.saveMessage(messageDto))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.BOARD_NOT_FOUND);

			verify(boardRepository).findById(999L);
			verify(memberRepository, never()).findById(anyLong());
			verify(chatMessageRepository, never()).save(any(ChatMessage.class));
		}

		@Test
		@DisplayName("존재하지 않는 발신자 - SENDER_NOT_FOUND 예외 발생")
		void saveMessage_SenderNotFound_ThrowsException() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(1L)
				.senderId(999L)
				.receiverId(2L)
				.content("메시지")
				.build();

			given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatService.saveMessage(messageDto))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.SENDER_NOT_FOUND);

			verify(boardRepository).findById(1L);
			verify(memberRepository).findById(999L);
			verify(chatMessageRepository, never()).save(any(ChatMessage.class));
		}

		@Test
		@DisplayName("존재하지 않는 수신자 - RECEIVER_NOT_FOUND 예외 발생")
		void saveMessage_ReceiverNotFound_ThrowsException() {
			// given
			ChatMessageDto messageDto = ChatMessageDto.builder()
				.boardId(1L)
				.senderId(1L)
				.receiverId(999L)
				.content("메시지")
				.build();

			given(boardRepository.findById(1L)).willReturn(Optional.of(testBoard));
			given(memberRepository.findById(1L)).willReturn(Optional.of(sender));
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatService.saveMessage(messageDto))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.RECEIVER_NOT_FOUND);

			verify(boardRepository).findById(1L);
			verify(memberRepository).findById(1L);
			verify(memberRepository).findById(999L);
			verify(chatMessageRepository, never()).save(any(ChatMessage.class));
		}
	}
}