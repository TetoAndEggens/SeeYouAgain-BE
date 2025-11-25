package tetoandeggens.seeyouagainbe.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

	@InjectMocks
	private ChatRoomService chatRoomService;

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	private Member sender;
	private Member receiver;
	private Board testBoard;
	private ChatRoom chatRoom;
	private ChatMessage chatMessage;

	@BeforeEach
	void setUp() {
		sender = mock(Member.class);
		lenient().when(sender.getId()).thenReturn(1L);
		lenient().when(sender.getNickName()).thenReturn("발신자");

		receiver = mock(Member.class);
		lenient().when(receiver.getId()).thenReturn(2L);
		lenient().when(receiver.getNickName()).thenReturn("수신자");

		testBoard = mock(Board.class);
		lenient().when(testBoard.getId()).thenReturn(1L);
		lenient().when(testBoard.getContentType()).thenReturn(ContentType.MISSING);

		chatRoom = mock(ChatRoom.class);
		lenient().when(chatRoom.getId()).thenReturn(1L);
		lenient().when(chatRoom.getBoard()).thenReturn(testBoard);
		lenient().when(chatRoom.getSender()).thenReturn(sender);
		lenient().when(chatRoom.getReceiver()).thenReturn(receiver);

		chatMessage = mock(ChatMessage.class);
		lenient().when(chatMessage.getId()).thenReturn(1L);
		lenient().when(chatMessage.getChatRoom()).thenReturn(chatRoom);
		lenient().when(chatMessage.getSender()).thenReturn(sender);
		lenient().when(chatMessage.getContent()).thenReturn("테스트 메시지");
		lenient().when(chatMessage.getIsRead()).thenReturn(false);
		lenient().when(chatMessage.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 20, 10, 0));
	}

	@Nested
	@DisplayName("채팅방 목록 조회 테스트")
	class GetChatRoomsTests {

		@Test
		@DisplayName("내 전체 채팅방 목록 조회 - 성공")
		void getMyChatRooms_Success() {
			// given
			Long memberId = 1L;
			CursorPageRequest request = new CursorPageRequest(null, 10);
			SortDirection sortDirection = SortDirection.LATEST;

			List<ChatRoomResponse> mockResponses = List.of(
				ChatRoomResponse.builder()
					.chatRoomId(1L)
					.boardId(1L)
					.boardTitle("분실물 찾습니다")
					.contentType(ContentType.MISSING)
					.senderId(1L)
					.receiverId(2L)
					.otherMemberNickname("수신자")
					.lastMessage("안녕하세요")
					.lastMessageTime(LocalDateTime.now())
					.unreadCount(3L)
					.build()
			);

			given(chatRoomRepository.findChatRoomsWithDetails(memberId, null, 10, sortDirection))
				.willReturn(mockResponses);

			// when
			ChatRoomListResponse result = chatRoomService.getMyChatRooms(
				memberId, request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.chatRooms().getData()).hasSize(1);
			assertThat(result.chatRooms().getData().get(0).chatRoomId()).isEqualTo(1L);
			assertThat(result.chatRooms().getData().get(0).otherMemberNickname()).isEqualTo("수신자");

			verify(chatRoomRepository).findChatRoomsWithDetails(memberId, null, 10, sortDirection);
		}

		@Test
		@DisplayName("읽지 않은 메시지가 있는 채팅방 목록 조회 - 성공")
		void getUnreadChatRooms_Success() {
			// given
			Long memberId = 1L;
			CursorPageRequest request = new CursorPageRequest(null, 10);
			SortDirection sortDirection = SortDirection.LATEST;

			List<ChatRoomResponse> mockResponses = List.of(
				ChatRoomResponse.builder()
					.chatRoomId(1L)
					.boardId(1L)
					.boardTitle("분실물 찾습니다")
					.contentType(ContentType.MISSING)
					.senderId(1L)
					.receiverId(2L)
					.otherMemberNickname("수신자")
					.lastMessage("확인 부탁드립니다")
					.lastMessageTime(LocalDateTime.now())
					.unreadCount(5L)
					.build()
			);

			given(chatRoomRepository.findUnreadChatRoomsWithDetails(memberId, null, 10, sortDirection))
				.willReturn(mockResponses);

			// when
			ChatRoomListResponse result = chatRoomService.getUnreadChatRooms(
				memberId, request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.chatRooms().getData()).hasSize(1);
			assertThat(result.chatRooms().getData().get(0).unreadCount()).isEqualTo(5L);

			verify(chatRoomRepository).findUnreadChatRoomsWithDetails(memberId, null, 10, sortDirection);
		}

		@Test
		@DisplayName("빈 채팅방 목록 조회 - 빈 리스트 반환")
		void getMyChatRooms_EmptyList() {
			// given
			Long memberId = 1L;
			CursorPageRequest request = new CursorPageRequest(null, 10);
			SortDirection sortDirection = SortDirection.LATEST;

			given(chatRoomRepository.findChatRoomsWithDetails(memberId, null, 10, sortDirection))
				.willReturn(new ArrayList<>());

			// when
			ChatRoomListResponse result = chatRoomService.getMyChatRooms(
				memberId, request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.chatRooms().getData()).isEmpty();
			assertThat(result.chatRooms().isHasNext()).isFalse();

			verify(chatRoomRepository).findChatRoomsWithDetails(memberId, null, 10, sortDirection);
		}
	}

	@Nested
	@DisplayName("채팅방 메시지 조회 테스트")
	class GetChatMessagesTests {

		@Test
		@DisplayName("채팅방 메시지 조회 - sender가 조회 성공")
		void getChatMessages_AsSender_Success() {
			// given
			Long chatRoomId = 1L;
			Long memberId = 1L; // sender
			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			List<ChatMessage> messages = List.of(chatMessage);

			given(chatRoomRepository.findByIdWithMembers(chatRoomId))
				.willReturn(Optional.of(chatRoom));
			given(chatMessageRepository.findMessagesByChatRoom(chatRoomId, null, 20, sortDirection))
				.willReturn(messages);

			// when
			ChatMessageListResponse result = chatRoomService.getChatMessages(
				chatRoomId, memberId, request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.messages().getData()).hasSize(1);
			assertThat(result.messages().getData().get(0).content()).isEqualTo("테스트 메시지");

			verify(chatRoomRepository).findByIdWithMembers(chatRoomId);
			verify(chatMessageRepository).markAsReadByChatRoomAndReceiver(chatRoomId, memberId);
			verify(chatMessageRepository).findMessagesByChatRoom(chatRoomId, null, 20, sortDirection);
		}

		@Test
		@DisplayName("채팅방 메시지 조회 - receiver가 조회 성공")
		void getChatMessages_AsReceiver_Success() {
			// given
			Long chatRoomId = 1L;
			Long memberId = 2L; // receiver
			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			List<ChatMessage> messages = List.of(chatMessage);

			given(chatRoomRepository.findByIdWithMembers(chatRoomId))
				.willReturn(Optional.of(chatRoom));
			given(chatMessageRepository.findMessagesByChatRoom(chatRoomId, null, 20, sortDirection))
				.willReturn(messages);

			// when
			ChatMessageListResponse result = chatRoomService.getChatMessages(
				chatRoomId, memberId, request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.messages().getData()).hasSize(1);

			verify(chatRoomRepository).findByIdWithMembers(chatRoomId);
			verify(chatMessageRepository).markAsReadByChatRoomAndReceiver(chatRoomId, memberId);
		}

		@Test
		@DisplayName("존재하지 않는 채팅방 조회 - CHAT_ROOM_NOT_FOUND 예외 발생")
		void getChatMessages_ChatRoomNotFound_ThrowsException() {
			// given
			Long chatRoomId = 999L;
			Long memberId = 1L;
			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			given(chatRoomRepository.findByIdWithMembers(chatRoomId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatRoomService.getChatMessages(
				chatRoomId, memberId, request, sortDirection
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_ROOM_NOT_FOUND);

			verify(chatRoomRepository).findByIdWithMembers(chatRoomId);
			verify(chatMessageRepository, never()).markAsReadByChatRoomAndReceiver(anyLong(), anyLong());
			verify(chatMessageRepository, never()).findMessagesByChatRoom(
				anyLong(), any(), anyInt(), any()
			);
		}

		@Test
		@DisplayName("권한 없는 사용자가 채팅방 조회 - CHAT_FORBIDDEN 예외 발생")
		void getChatMessages_Forbidden_ThrowsException() {
			// given
			Long chatRoomId = 1L;
			Long unauthorizedMemberId = 999L; // sender도 receiver도 아님
			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			given(chatRoomRepository.findByIdWithMembers(chatRoomId))
				.willReturn(Optional.of(chatRoom));

			// when & then
			assertThatThrownBy(() -> chatRoomService.getChatMessages(
				chatRoomId, unauthorizedMemberId, request, sortDirection
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_FORBIDDEN);

			verify(chatRoomRepository).findByIdWithMembers(chatRoomId);
			verify(chatMessageRepository, never()).markAsReadByChatRoomAndReceiver(anyLong(), anyLong());
			verify(chatMessageRepository, never()).findMessagesByChatRoom(
				anyLong(), any(), anyInt(), any()
			);
		}
	}
}