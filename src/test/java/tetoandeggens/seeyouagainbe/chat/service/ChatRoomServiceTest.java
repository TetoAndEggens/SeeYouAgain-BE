package tetoandeggens.seeyouagainbe.chat.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomListResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("ChatRoomService 통합 테스트")
class ChatRoomServiceTest extends ServiceTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member sender;
	private Member receiver;
	private Member thirdMember;
	private Board testBoard;
	private ChatRoom chatRoom;

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

		thirdMember = memberRepository.save(Member.builder()
			.loginId("third123")
			.password("password")
			.nickName("제3자")
			.phoneNumber("01099999999")
			.build());

		testBoard = boardRepository.save(Board.builder()
			.member(sender)
			.title("테스트 게시글")
			.content("테스트 내용")
			.contentType(ContentType.MISSING)
			.build());

		chatRoom = chatRoomRepository.save(ChatRoom.builder()
			.board(testBoard)
			.sender(sender)
			.receiver(receiver)
			.build());
	}

	@Nested
	@DisplayName("채팅방 목록 조회 테스트")
	class GetChatRoomsTests {

		@Test
		@DisplayName("내 전체 채팅방 목록 조회 - 성공")
		void getMyChatRooms_Success() {
			// given
			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("테스트 메시지")
				.build());

			CursorPageRequest request = new CursorPageRequest(null, 10);
			SortDirection sortDirection = SortDirection.LATEST;

			// when
			ChatRoomListResponse result = chatRoomService.getMyChatRooms(
				sender.getId(), request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.chatRooms().getData()).hasSize(1);
			assertThat(result.chatRooms().getData().get(0).chatRoomId()).isEqualTo(chatRoom.getId());
		}

		@Test
		@DisplayName("읽지 않은 메시지가 있는 채팅방 목록 조회 - 성공")
		void getUnreadChatRooms_Success() {
			// given
			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("확인 부탁드립니다")
				.build());

			CursorPageRequest request = new CursorPageRequest(null, 10);
			SortDirection sortDirection = SortDirection.LATEST;

			// when
			ChatRoomListResponse result = chatRoomService.getUnreadChatRooms(
				receiver.getId(), request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.chatRooms().getData()).hasSize(1);
			assertThat(result.chatRooms().getData().get(0).unreadCount()).isGreaterThan(0L);
		}

		@Test
		@DisplayName("빈 채팅방 목록 조회 - 빈 리스트 반환")
		void getMyChatRooms_EmptyList() {
			// given
			CursorPageRequest request = new CursorPageRequest(null, 10);
			SortDirection sortDirection = SortDirection.LATEST;

			// when
			ChatRoomListResponse result = chatRoomService.getMyChatRooms(
				thirdMember.getId(), request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.chatRooms().getData()).isEmpty();
			assertThat(result.chatRooms().isHasNext()).isFalse();
		}
	}

	@Nested
	@DisplayName("채팅방 메시지 조회 테스트")
	class GetChatMessagesTests {

		@Test
		@DisplayName("채팅방 메시지 조회 - sender가 조회 성공")
		void getChatMessages_AsSender_Success() {
			// given
			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("테스트 메시지")
				.build());

			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			// when
			ChatMessageListResponse result = chatRoomService.getChatMessages(
				chatRoom.getId(), sender.getId(), request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.messages().getData()).hasSize(1);
			assertThat(result.messages().getData().get(0).content()).isEqualTo("테스트 메시지");
		}

		@Test
		@DisplayName("채팅방 메시지 조회 - receiver가 조회 성공")
		void getChatMessages_AsReceiver_Success() {
			// given
			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("테스트 메시지")
				.build());

			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			// when
			ChatMessageListResponse result = chatRoomService.getChatMessages(
				chatRoom.getId(), receiver.getId(), request, sortDirection
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.messages().getData()).hasSize(1);
		}

		@Test
		@DisplayName("존재하지 않는 채팅방 조회 - CHAT_ROOM_NOT_FOUND 예외 발생")
		void getChatMessages_ChatRoomNotFound_ThrowsException() {
			// given
			Long nonExistentChatRoomId = 999L;
			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			// when & then
			assertThatThrownBy(() -> chatRoomService.getChatMessages(
				nonExistentChatRoomId, sender.getId(), request, sortDirection
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		@Test
		@DisplayName("권한 없는 사용자가 채팅방 조회 - CHAT_FORBIDDEN 예외 발생")
		void getChatMessages_Forbidden_ThrowsException() {
			// given
			CursorPageRequest request = new CursorPageRequest(null, 20);
			SortDirection sortDirection = SortDirection.LATEST;

			// when & then
			assertThatThrownBy(() -> chatRoomService.getChatMessages(
				chatRoom.getId(), thirdMember.getId(), request, sortDirection
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_FORBIDDEN);
		}
	}
}