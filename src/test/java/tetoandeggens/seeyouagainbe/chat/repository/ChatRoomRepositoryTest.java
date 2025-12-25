package tetoandeggens.seeyouagainbe.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("ChatRoomRepository 단위 테스트")
class ChatRoomRepositoryTest extends RepositoryTest {

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BoardRepository boardRepository;

	private Member sender;
	private Member receiver;
	private Member otherMember;
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
			.nickName("발신자")
			.phoneNumber("01011111111")
			.password("password")
			.build());

		receiver = memberRepository.save(Member.builder()
			.loginId("receiver123")
			.nickName("수신자")
			.phoneNumber("01022222222")
			.password("password")
			.build());

		otherMember = memberRepository.save(Member.builder()
			.loginId("other123")
			.nickName("다른사람")
			.phoneNumber("01033333333")
			.password("password")
			.build());

		testBoard = boardRepository.save(Board.builder()
			.contentType(ContentType.MISSING)
			.title("분실물 찾습니다")
			.content("지갑을 잃어버렸습니다")
			.member(sender)
			.build());

		chatRoom = chatRoomRepository.save(ChatRoom.builder()
			.board(testBoard)
			.sender(sender)
			.receiver(receiver)
			.violatedStatus(ViolatedStatus.NORMAL)
			.build());
	}

	@Nested
	@DisplayName("채팅방 조회 테스트")
	class FindChatRoomTests {

		@Test
		@DisplayName("게시물과 멤버로 채팅방 ID 찾기 - 성공")
		void findChatRoomIdByBoardAndMembers_Success() {
			// when
			Optional<Long> result = chatRoomRepository.findChatRoomIdByBoardAndMembers(
				testBoard.getId(), sender.getId(), receiver.getId()
			);

			// then
			assertThat(result).isPresent();
			assertThat(result.get()).isEqualTo(chatRoom.getId());
		}

		@Test
		@DisplayName("존재하지 않는 채팅방 조회 - 빈 Optional 반환")
		void findChatRoomIdByBoardAndMembers_NotFound() {
			// when
			Optional<Long> result = chatRoomRepository.findChatRoomIdByBoardAndMembers(
				testBoard.getId(), sender.getId(), otherMember.getId()
			);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("ID로 채팅방과 멤버 함께 조회 - 성공")
		void findByIdWithMembers_Success() {
			// when
			Optional<ChatRoom> result = chatRoomRepository.findByIdWithMembers(chatRoom.getId());

			// then
			assertThat(result).isPresent();
			assertThat(result.get().getSender().getNickName()).isEqualTo("발신자");
			assertThat(result.get().getReceiver().getNickName()).isEqualTo("수신자");
		}

		@Test
		@DisplayName("ID와 멤버로 채팅방 조회 및 권한 검증 - sender 성공")
		void findByIdWithMembersAndValidateAccess_AsSender_Success() {
			// when
			Optional<ChatRoom> result = chatRoomRepository.findByIdWithMembersAndValidateAccess(
				chatRoom.getId(), sender.getId()
			);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().getId()).isEqualTo(chatRoom.getId());
		}

		@Test
		@DisplayName("ID와 멤버로 채팅방 조회 및 권한 검증 - receiver 성공")
		void findByIdWithMembersAndValidateAccess_AsReceiver_Success() {
			// when
			Optional<ChatRoom> result = chatRoomRepository.findByIdWithMembersAndValidateAccess(
				chatRoom.getId(), receiver.getId()
			);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().getId()).isEqualTo(chatRoom.getId());
		}

		@Test
		@DisplayName("ID와 멤버로 채팅방 조회 및 권한 검증 - 권한 없는 사용자는 빈 Optional")
		void findByIdWithMembersAndValidateAccess_Unauthorized_ReturnsEmpty() {
			// when
			Optional<ChatRoom> result = chatRoomRepository.findByIdWithMembersAndValidateAccess(
				chatRoom.getId(), otherMember.getId()
			);

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("채팅방 목록 조회 테스트")
	class FindChatRoomListTests {

		@BeforeEach
		void setUpChatRooms() {
			Board board2 = boardRepository.save(Board.builder()
				.contentType(ContentType.WITNESS)
				.title("목격 정보 있습니다")
				.content("핸드폰 주웠습니다")
				.member(receiver)
				.build());

			ChatRoom chatRoom2 = chatRoomRepository.save(ChatRoom.builder()
				.board(board2)
				.sender(receiver)
				.receiver(sender)
				.violatedStatus(ViolatedStatus.NORMAL)
				.build());

			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("첫 번째 메시지")
				.build());

			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom2)
				.sender(receiver)
				.receiver(sender)
				.content("두 번째 메시지")
				.build());

			chatRoom.updateLastMessageAt(LocalDateTime.now().minusHours(1));
			chatRoom2.updateLastMessageAt(LocalDateTime.now());
			chatRoomRepository.save(chatRoom);
			chatRoomRepository.save(chatRoom2);
		}

		@Test
		@DisplayName("내 채팅방 목록 조회 - 성공")
		void findChatRoomsWithDetails_Success() {
			// when
			List<ChatRoomResponse> results = chatRoomRepository.findChatRoomsWithDetails(
				sender.getId(), null, 10, SortDirection.LATEST
			);

			// then
			assertThat(results).isNotEmpty();
			assertThat(results).hasSizeGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("내 채팅방 목록 조회 - 최신순 정렬")
		void findChatRoomsWithDetails_SortedByLatest() {
			// when
			List<ChatRoomResponse> results = chatRoomRepository.findChatRoomsWithDetails(
				sender.getId(), null, 10, SortDirection.LATEST
			);

			// then
			assertThat(results).isNotEmpty();
			if (results.size() >= 2) {
				assertThat(results.get(0).lastMessageTime())
					.isAfterOrEqualTo(results.get(1).lastMessageTime());
			}
		}

		@Test
		@DisplayName("내 채팅방 목록 조회 - 커서 기반 페이징")
		void findChatRoomsWithDetails_WithCursor() {
			// given
			List<ChatRoomResponse> firstPage = chatRoomRepository.findChatRoomsWithDetails(
				sender.getId(), null, 1, SortDirection.LATEST
			);

			assertThat(firstPage).hasSizeLessThanOrEqualTo(2);
			assertThat(firstPage).hasSizeGreaterThanOrEqualTo(1);
			Long cursorId = firstPage.get(0).chatRoomId();

			// when
			List<ChatRoomResponse> secondPage = chatRoomRepository.findChatRoomsWithDetails(
				sender.getId(), cursorId, 1, SortDirection.LATEST
			);

			// then
			assertThat(secondPage).isNotEmpty();
			assertThat(secondPage.get(0).chatRoomId()).isNotEqualTo(cursorId);
		}

		@Test
		@DisplayName("읽지 않은 메시지가 있는 채팅방 목록 조회 - 성공")
		void findUnreadChatRoomsWithDetails_Success() {
			// when
			List<ChatRoomResponse> results = chatRoomRepository.findUnreadChatRoomsWithDetails(
				sender.getId(), null, 10, SortDirection.LATEST
			);

			// then
			assertThat(results).allMatch(room -> room.unreadCount() > 0);
		}
	}
}