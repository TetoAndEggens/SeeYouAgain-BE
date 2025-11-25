package tetoandeggens.seeyouagainbe.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

@DisplayName("ChatMessageRepository 단위 테스트")
class ChatMessageRepositoryTest extends RepositoryTest {

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BoardRepository boardRepository;

	@PersistenceContext
	private EntityManager entityManager;

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
			.contentType(ContentType.MISSING)
			.violatedStatus(ViolatedStatus.NORMAL)
			.build());
	}

	@Nested
	@DisplayName("메시지 조회 테스트")
	class FindMessagesTests {

		@BeforeEach
		void setUpMessages() {
			for (int i = 1; i <= 5; i++) {
				Member msgSender = i % 2 == 0 ? sender : receiver;
				Member msgReceiver = i % 2 == 0 ? receiver : sender;
				chatMessageRepository.save(ChatMessage.builder()
					.chatRoom(chatRoom)
					.sender(msgSender)
					.receiver(msgReceiver)
					.content("메시지 " + i)
					.build());
			}
		}

		@Test
		@DisplayName("채팅방의 메시지 조회 - 성공")
		void findMessagesByChatRoom_Success() {
			// when
			List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), null, 10, SortDirection.LATEST
			);

			// then
			assertThat(messages).hasSize(5);
		}

		@Test
		@DisplayName("채팅방의 메시지 조회 - 최신순 정렬")
		void findMessagesByChatRoom_SortedByLatest() {
			// when
			List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), null, 10, SortDirection.LATEST
			);

			// then
			assertThat(messages).isNotEmpty();
			if (messages.size() >= 2) {
				assertThat(messages.get(0).getCreatedAt())
					.isAfterOrEqualTo(messages.get(1).getCreatedAt());
			}
		}

		@Test
		@DisplayName("채팅방의 메시지 조회 - 커서 기반 페이징")
		void findMessagesByChatRoom_WithCursor() {
			// given
			List<ChatMessage> firstPage = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), null, 2, SortDirection.LATEST
			);

			assertThat(firstPage).hasSizeLessThanOrEqualTo(3);
			assertThat(firstPage).hasSizeGreaterThanOrEqualTo(2);
			Long cursorId = firstPage.get(1).getId();

			// when
			List<ChatMessage> secondPage = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), cursorId, 2, SortDirection.LATEST
			);

			// then
			assertThat(secondPage).isNotEmpty();
			assertThat(secondPage).allMatch(msg -> !msg.getId().equals(cursorId));
		}

		@Test
		@DisplayName("채팅방의 메시지 조회 - size 제한")
		void findMessagesByChatRoom_WithSizeLimit() {
			// when
			List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), null, 3, SortDirection.LATEST
			);

			// then
			assertThat(messages).hasSizeLessThanOrEqualTo(4);
		}

		@Test
		@DisplayName("빈 채팅방의 메시지 조회 - 빈 리스트 반환")
		void findMessagesByChatRoom_EmptyChatRoom() {
			// given
			ChatRoom emptyChatRoom = chatRoomRepository.save(ChatRoom.builder()
				.board(testBoard)
				.sender(sender)
				.receiver(otherMember)
				.contentType(ContentType.MISSING)
				.violatedStatus(ViolatedStatus.NORMAL)
				.build());

			// when
			List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
				emptyChatRoom.getId(), null, 10, SortDirection.LATEST
			);

			// then
			assertThat(messages).isEmpty();
		}
	}

	@Nested
	@DisplayName("메시지 읽음 처리 테스트")
	class MarkAsReadTests {

		@Test
		@DisplayName("채팅방의 읽지 않은 메시지 읽음 처리 - 성공")
		void markAsReadByChatRoomAndReceiver_Success() {
			// given
			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("읽지 않은 메시지 1")
				.build());

			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("읽지 않은 메시지 2")
				.build());

			// when
			chatMessageRepository.markAsReadByChatRoomAndReceiver(chatRoom.getId(), receiver.getId());
			entityManager.flush();
			entityManager.clear();

			// then
			List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), null, 10, SortDirection.LATEST
			);

			assertThat(messages)
				.filteredOn(msg -> msg.getSender().getId().equals(sender.getId()))
				.allMatch(ChatMessage::getIsRead);
		}

		@Test
		@DisplayName("자신이 보낸 메시지는 읽음 처리되지 않음")
		void markAsReadByChatRoomAndReceiver_DoesNotMarkOwnMessages() {
			// given
			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("상대방 메시지")
				.build());

			chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(receiver)
				.receiver(sender)
				.content("내 메시지")
				.build());

			// when
			chatMessageRepository.markAsReadByChatRoomAndReceiver(chatRoom.getId(), receiver.getId());

			// then
			List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoom(
				chatRoom.getId(), null, 10, SortDirection.LATEST
			);

			ChatMessage receiverMessage = messages.stream()
				.filter(msg -> msg.getSender().getId().equals(receiver.getId()))
				.findFirst()
				.orElseThrow();

			assertThat(receiverMessage.getIsRead()).isFalse();
		}
	}

	@Nested
	@DisplayName("메시지 권한 검증 조회 테스트")
	class FindWithValidationTests {

		@Test
		@DisplayName("메시지 ID와 멤버로 조회 및 권한 검증 - sender 성공")
		void findByIdWithChatRoomAndMembersAndValidateAccess_AsSender_Success() {
			// given
			ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
					.build());

			// when
			Optional<ChatMessage> result = chatMessageRepository
				.findByIdWithChatRoomAndMembersAndValidateAccess(message.getId(), sender.getId());

			// then
			assertThat(result).isPresent();
			assertThat(result.get().getId()).isEqualTo(message.getId());
		}

		@Test
		@DisplayName("메시지 ID와 멤버로 조회 및 권한 검증 - receiver 성공")
		void findByIdWithChatRoomAndMembersAndValidateAccess_AsReceiver_Success() {
			// given
			ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
					.build());

			// when
			Optional<ChatMessage> result = chatMessageRepository
				.findByIdWithChatRoomAndMembersAndValidateAccess(message.getId(), receiver.getId());

			// then
			assertThat(result).isPresent();
			assertThat(result.get().getId()).isEqualTo(message.getId());
		}

		@Test
		@DisplayName("메시지 ID와 멤버로 조회 및 권한 검증 - 권한 없는 사용자는 빈 Optional")
		void findByIdWithChatRoomAndMembersAndValidateAccess_Unauthorized_ReturnsEmpty() {
			// given
			ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
					.build());

			// when
			Optional<ChatMessage> result = chatMessageRepository
				.findByIdWithChatRoomAndMembersAndValidateAccess(message.getId(), otherMember.getId());

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 메시지 ID 조회 - 빈 Optional 반환")
		void findByIdWithChatRoomAndMembersAndValidateAccess_NotFound() {
			// when
			Optional<ChatMessage> result = chatMessageRepository
				.findByIdWithChatRoomAndMembersAndValidateAccess(999L, sender.getId());

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("메시지 타입별 저장 및 조회 테스트")
	class MessageTypeTests {

		@Test
		@DisplayName("텍스트 메시지 저장 및 조회 - 성공")
		void saveAndFind_TextMessage() {
			// given
			ChatMessage textMessage = chatMessageRepository.save(ChatMessage.builder()
				.chatRoom(chatRoom)
				.sender(sender)
				.receiver(receiver)
				.content("텍스트 메시지입니다")
				.build());

			// when
			ChatMessage found = chatMessageRepository.findById(textMessage.getId()).orElseThrow();

			// then
				assertThat(found.getContent()).isEqualTo("텍스트 메시지입니다");
		}
	}
}