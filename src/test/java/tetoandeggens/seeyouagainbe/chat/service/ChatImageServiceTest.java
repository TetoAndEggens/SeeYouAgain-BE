package tetoandeggens.seeyouagainbe.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.chat.dto.response.ImageUrlResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.UploadImageResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.entity.MessageType;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.image.service.ImageService;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@DisplayName("ChatImageService 단위 테스트")
class ChatImageServiceTest extends ServiceTest {

	@Autowired
	private ChatImageService chatImageService;

	@MockitoBean
	private ChatRoomRepository chatRoomRepository;

	@MockitoBean
	private ChatMessageRepository chatMessageRepository;

	@MockitoBean
	private ImageService imageService;

	private Member sender;
	private Member receiver;
	private Board testBoard;
	private ChatRoom chatRoom;
	private ChatMessage imageMessage;

	@BeforeEach
	void setUp() {
		sender = mock(Member.class);
		given(sender.getId()).willReturn(1L);

		receiver = mock(Member.class);
		given(receiver.getId()).willReturn(2L);

		testBoard = mock(Board.class);
		given(testBoard.getId()).willReturn(1L);

		chatRoom = mock(ChatRoom.class);
		given(chatRoom.getId()).willReturn(1L);
		given(chatRoom.getSender()).willReturn(sender);
		given(chatRoom.getReceiver()).willReturn(receiver);

		imageMessage = mock(ChatMessage.class);
		given(imageMessage.getId()).willReturn(1L);
		given(imageMessage.getChatRoom()).willReturn(chatRoom);
		given(imageMessage.getSender()).willReturn(sender);
		given(imageMessage.getMessageType()).willReturn(MessageType.IMAGE);
		given(imageMessage.getImageKey()).willReturn("chat-images/1/uuid_image.jpg");
	}

	@Nested
	@DisplayName("이미지 업로드 URL 생성 테스트")
	class GenerateUploadUrlTests {

		@Test
		@DisplayName("이미지 업로드 URL 생성 - sender 권한으로 성공")
		void generateUploadUrl_AsSender_Success() {
			// given
			Long memberId = 1L; // sender
			Long chatRoomId = 1L;
			String fileName = "test-image.jpg";
			String fileType = "image/jpeg";
			String mockPresignedUrl = "https://s3.amazonaws.com/bucket/chat-images/1/uuid_test-image.jpg?signature=xxx";

			given(chatRoomRepository.findByIdWithMembersAndValidateAccess(chatRoomId, memberId))
				.willReturn(Optional.of(chatRoom));
			given(imageService.generateUploadPresignedUrl(anyString(), eq(fileType), eq(Duration.ofMinutes(5))))
				.willReturn(mockPresignedUrl);

			// when
			UploadImageResponse result = chatImageService.generateUploadUrlWithValidation(
				memberId, chatRoomId, fileName, fileType
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.url()).isEqualTo(mockPresignedUrl);
			assertThat(result.imageS3Key()).contains("chat-images/1/");
			assertThat(result.imageS3Key()).contains(fileName);

			verify(chatRoomRepository).findByIdWithMembersAndValidateAccess(chatRoomId, memberId);
			verify(imageService).generateUploadPresignedUrl(anyString(), eq(fileType), eq(Duration.ofMinutes(5)));
		}

		@Test
		@DisplayName("이미지 업로드 URL 생성 - receiver 권한으로 성공")
		void generateUploadUrl_AsReceiver_Success() {
			// given
			Long memberId = 2L; // receiver
			Long chatRoomId = 1L;
			String fileName = "photo.png";
			String fileType = "image/png";
			String mockPresignedUrl = "https://s3.amazonaws.com/bucket/chat-images/1/uuid_photo.png?signature=xxx";

			given(chatRoomRepository.findByIdWithMembersAndValidateAccess(chatRoomId, memberId))
				.willReturn(Optional.of(chatRoom));
			given(imageService.generateUploadPresignedUrl(anyString(), eq(fileType), eq(Duration.ofMinutes(5))))
				.willReturn(mockPresignedUrl);

			// when
			UploadImageResponse result = chatImageService.generateUploadUrlWithValidation(
				memberId, chatRoomId, fileName, fileType
			);

			// then
			assertThat(result).isNotNull();
			assertThat(result.url()).isEqualTo(mockPresignedUrl);

			verify(chatRoomRepository).findByIdWithMembersAndValidateAccess(chatRoomId, memberId);
			verify(imageService).generateUploadPresignedUrl(anyString(), eq(fileType), eq(Duration.ofMinutes(5)));
		}

		@Test
		@DisplayName("이미지 업로드 URL 생성 - 권한 없는 사용자 - CHAT_FORBIDDEN 예외 발생")
		void generateUploadUrl_Forbidden_ThrowsException() {
			// given
			Long unauthorizedMemberId = 999L;
			Long chatRoomId = 1L;
			String fileName = "test-image.jpg";
			String fileType = "image/jpeg";

			given(chatRoomRepository.findByIdWithMembersAndValidateAccess(chatRoomId, unauthorizedMemberId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatImageService.generateUploadUrlWithValidation(
				unauthorizedMemberId, chatRoomId, fileName, fileType
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_FORBIDDEN);

			verify(chatRoomRepository).findByIdWithMembersAndValidateAccess(chatRoomId, unauthorizedMemberId);
			verify(imageService, never()).generateUploadPresignedUrl(anyString(), anyString(), any(Duration.class));
		}
	}

	@Nested
	@DisplayName("이미지 다운로드 URL 생성 테스트")
	class GenerateDownloadUrlTests {

		@Test
		@DisplayName("이미지 다운로드 URL 생성 - 권한 있는 사용자 성공")
		void generateDownloadUrl_Authorized_Success() {
			// given
			Long memberId = 1L; // sender
			Long messageId = 1L;
			String mockDownloadUrl = "https://s3.amazonaws.com/bucket/chat-images/1/uuid_image.jpg?signature=xxx";

			given(chatMessageRepository.findByIdWithChatRoomAndMembersAndValidateAccess(messageId, memberId))
				.willReturn(Optional.of(imageMessage));
			given(imageService.generateDownloadPresignedUrl(imageMessage.getImageKey(), Duration.ofHours(1)))
				.willReturn(mockDownloadUrl);

			// when
			ImageUrlResponse result = chatImageService.generateDownloadUrlWithValidation(memberId, messageId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.url()).isEqualTo(mockDownloadUrl);

			verify(chatMessageRepository).findByIdWithChatRoomAndMembersAndValidateAccess(messageId, memberId);
			verify(imageService).generateDownloadPresignedUrl(imageMessage.getImageKey(), Duration.ofHours(1));
		}

		@Test
		@DisplayName("이미지 다운로드 URL 생성 - receiver도 성공")
		void generateDownloadUrl_AsReceiver_Success() {
			// given
			Long memberId = 2L; // receiver
			Long messageId = 1L;
			String mockDownloadUrl = "https://s3.amazonaws.com/bucket/chat-images/1/uuid_image.jpg?signature=xxx";

			given(chatMessageRepository.findByIdWithChatRoomAndMembersAndValidateAccess(messageId, memberId))
				.willReturn(Optional.of(imageMessage));
			given(imageService.generateDownloadPresignedUrl(imageMessage.getImageKey(), Duration.ofHours(1)))
				.willReturn(mockDownloadUrl);

			// when
			ImageUrlResponse result = chatImageService.generateDownloadUrlWithValidation(memberId, messageId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.url()).isEqualTo(mockDownloadUrl);

			verify(chatMessageRepository).findByIdWithChatRoomAndMembersAndValidateAccess(messageId, memberId);
			verify(imageService).generateDownloadPresignedUrl(anyString(), eq(Duration.ofHours(1)));
		}

		@Test
		@DisplayName("이미지 다운로드 URL 생성 - 권한 없는 사용자 - CHAT_FORBIDDEN 예외 발생")
		void generateDownloadUrl_Forbidden_ThrowsException() {
			// given
			Long unauthorizedMemberId = 999L;
			Long messageId = 1L;

			given(
				chatMessageRepository.findByIdWithChatRoomAndMembersAndValidateAccess(messageId, unauthorizedMemberId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatImageService.generateDownloadUrlWithValidation(
				unauthorizedMemberId, messageId
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_FORBIDDEN);

			verify(chatMessageRepository).findByIdWithChatRoomAndMembersAndValidateAccess(messageId,
				unauthorizedMemberId);
			verify(imageService, never()).generateDownloadPresignedUrl(anyString(), any(Duration.class));
		}

		@Test
		@DisplayName("이미지 다운로드 URL 생성 - 존재하지 않는 메시지 - 예외 발생")
		void generateDownloadUrl_MessageNotFound_ThrowsException() {
			// given
			Long memberId = 1L;
			Long nonExistentMessageId = 999L;

			given(chatMessageRepository.findByIdWithChatRoomAndMembersAndValidateAccess(nonExistentMessageId, memberId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> chatImageService.generateDownloadUrlWithValidation(
				memberId, nonExistentMessageId
			))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_FORBIDDEN);

			verify(chatMessageRepository).findByIdWithChatRoomAndMembersAndValidateAccess(nonExistentMessageId,
				memberId);
			verify(imageService, never()).generateDownloadPresignedUrl(anyString(), any(Duration.class));
		}
	}
}
