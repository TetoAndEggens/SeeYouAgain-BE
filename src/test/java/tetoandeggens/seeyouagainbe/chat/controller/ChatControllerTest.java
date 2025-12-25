package tetoandeggens.seeyouagainbe.chat.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.service.ChatRoomService;
import tetoandeggens.seeyouagainbe.chat.service.ChatService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.global.ControllerTest;

@WebMvcTest(ChatController.class)
@DisplayName("ChatController 통합 테스트")
class ChatControllerTest extends ControllerTest {

	@MockitoBean
	private ChatRoomService chatRoomService;

	@MockitoBean
	private ChatService chatService;

	@Nested
	@DisplayName("채팅방 목록 조회 API 테스트")
	class GetChatRoomsTests {

		@Test
		@DisplayName("GET /chat/rooms - 내 전체 채팅방 목록 조회 성공")
		void getMyChatRooms_Success() throws Exception {
			// given
			Long memberId = 1L;
			List<ChatRoomResponse> responses = List.of(
				ChatRoomResponse.builder()
					.chatRoomId(1L)
					.boardId(1L)
					.boardTitle("분실물 찾습니다")
					.contentType(ContentType.MISSING)
					.senderId(1L)
					.receiverId(2L)
					.otherMemberNickname("수신자")
					.lastMessage("안녕하세요")
					.lastMessageTime(LocalDateTime.of(2025, 1, 20, 14, 30))
					.unreadCount(3L)
					.build()
			);

			CursorPage<ChatRoomResponse, Long> page = CursorPage.of(responses, 10, ChatRoomResponse::chatRoomId);
			ChatRoomListResponse response = ChatRoomListResponse.of(page);

			given(chatRoomService.getMyChatRooms(eq(memberId), any(CursorPageRequest.class), eq(SortDirection.LATEST)))
				.willReturn(response);

			// when & then
			mockMvc.perform(get("/chat/rooms")
					.param("size", "10")
					.param("sortDirection", "LATEST")
					.with(mockUser(memberId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chatRooms.data").isArray())
				.andExpect(jsonPath("$.chatRooms.data[0].chatRoomId").value(1))
				.andExpect(jsonPath("$.chatRooms.data[0].boardTitle").value("분실물 찾습니다"))
				.andExpect(jsonPath("$.chatRooms.data[0].otherMemberNickname").value("수신자"))
				.andExpect(jsonPath("$.chatRooms.data[0].unreadCount").value(3));

			verify(chatRoomService).getMyChatRooms(eq(memberId), any(CursorPageRequest.class),
				eq(SortDirection.LATEST));
		}

		@Test
		@DisplayName("GET /chat/rooms/unread - 읽지 않은 메시지가 있는 채팅방 목록 조회 성공")
		void getUnreadChatRooms_Success() throws Exception {
			// given
			Long memberId = 1L;
			List<ChatRoomResponse> responses = List.of(
				ChatRoomResponse.builder()
					.chatRoomId(1L)
					.boardId(1L)
					.boardTitle("목격 정보 공유")
					.contentType(ContentType.WITNESS)
					.senderId(1L)
					.receiverId(2L)
					.otherMemberNickname("홍길동")
					.lastMessage("확인 부탁드립니다")
					.lastMessageTime(LocalDateTime.of(2025, 1, 20, 15, 0))
					.unreadCount(5L)
					.build()
			);

			CursorPage<ChatRoomResponse, Long> page = CursorPage.of(responses, 10, ChatRoomResponse::chatRoomId);
			ChatRoomListResponse response = ChatRoomListResponse.of(page);

			given(chatRoomService.getUnreadChatRooms(eq(memberId), any(CursorPageRequest.class),
				eq(SortDirection.LATEST)))
				.willReturn(response);

			// when & then
			mockMvc.perform(get("/chat/rooms/unread")
					.param("size", "10")
					.param("sortDirection", "LATEST")
					.with(mockUser(memberId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chatRooms.data").isArray())
				.andExpect(jsonPath("$.chatRooms.data[0].chatRoomId").value(1))
				.andExpect(jsonPath("$.chatRooms.data[0].unreadCount").value(5));

			verify(chatRoomService).getUnreadChatRooms(eq(memberId), any(CursorPageRequest.class),
				eq(SortDirection.LATEST));
		}
	}

	@Nested
	@DisplayName("채팅방 메시지 조회 API 테스트")
	class GetChatMessagesTests {

		@Test
		@DisplayName("GET /chat/rooms/{chatRoomId}/messages - 채팅방 메시지 조회 성공")
		void getChatMessages_Success() throws Exception {
			// given
			Long memberId = 1L;
			Long chatRoomId = 1L;
			List<ChatMessageResponse> messages = List.of(
				ChatMessageResponse.builder()
					.messageId(1L)
					.senderId(1L)
					.content("안녕하세요")
					.isRead(true)
					.createdAt("2025-01-20T14:30:00")
					.build()
			);

			CursorPage<ChatMessageResponse, Long> page = CursorPage.of(messages, 20, ChatMessageResponse::messageId);
			ChatMessageListResponse response = ChatMessageListResponse.of(page);

			given(chatRoomService.getChatMessages(
				eq(chatRoomId), eq(memberId), any(CursorPageRequest.class), eq(SortDirection.LATEST)
			)).willReturn(response);

			// when & then
			mockMvc.perform(get("/chat/rooms/{chatRoomId}/messages", chatRoomId)
					.param("size", "20")
					.param("sortDirection", "LATEST")
					.with(mockUser(memberId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages.data").isArray())
				.andExpect(jsonPath("$.messages.data[0].messageId").value(1))
				.andExpect(jsonPath("$.messages.data[0].content").value("안녕하세요"));

			verify(chatRoomService).getChatMessages(
				eq(chatRoomId), eq(memberId), any(CursorPageRequest.class), eq(SortDirection.LATEST)
			);
		}
	}
}