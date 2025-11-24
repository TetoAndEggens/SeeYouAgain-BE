package tetoandeggens.seeyouagainbe.chat.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.chat.dto.request.UploadImageRequest;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ImageUrlResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.UploadImageResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.chat.service.ChatImageService;
import tetoandeggens.seeyouagainbe.chat.service.ChatRoomService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;

@Slf4j
@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatImageService chatImageService;
	private final ChatRoomService chatRoomService;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Operation(summary = "내 전체 채팅방 목록 조회",
		description = "내가 참여한 모든 채팅방 목록을 조회")
	@GetMapping("/rooms")
	public ResponseEntity<CursorPage<ChatRoomResponse, Long>> getMyChatRooms(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection
	) {
		CursorPage<ChatRoomResponse, Long> response = chatRoomService.getMyChatRooms(
			customUserDetails.getMemberId(),
			request,
			sortDirection
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "읽지 않은 메시지가 있는 채팅방 목록 조회",
		description = "읽지 않은 메시지가 있는 채팅방 목록만 조회")
	@GetMapping("/rooms/unread")
	public ResponseEntity<CursorPage<ChatRoomResponse, Long>> getUnreadChatRooms(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection
	) {
		CursorPage<ChatRoomResponse, Long> response = chatRoomService.getUnreadChatRooms(
			customUserDetails.getMemberId(),
			request,
			sortDirection
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "채팅방 메시지 조회",
		description = "채팅방의 메시지를 커서 기반 페이지네이션으로 조회하고 읽지 않은 메시지를 읽음 처리")
	@GetMapping("/rooms/{chatRoomId}/messages")
	public ResponseEntity<CursorPage<ChatMessageResponse, Long>> getChatMessages(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@PathVariable Long chatRoomId,
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection
	) {
		CursorPage<ChatMessageResponse, Long> response = chatRoomService.getChatMessages(
			chatRoomId,
			customUserDetails.getMemberId(),
			request,
			sortDirection
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "이미지 업로드 URL 발급", description = "채팅 이미지 업로드를 위한 Presigned URL을 발급합니다.")
	@PostMapping("/upload-url")
	public ResponseEntity<UploadImageResponse> getUploadUrl(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody UploadImageRequest request
	) {
		Long userId = userDetails.getMemberId();

		ChatRoom chatRoom = chatRoomRepository.findById(request.chatRoomId())
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

		if (!chatRoom.getSender().getId().equals(userId) && !chatRoom.getReceiver().getId().equals(userId)) {
			throw new CustomException(ChatErrorCode.CHAT_FORBIDDEN);
		}

		UploadImageResponse response = chatImageService.generateUploadUrl(
			request.chatRoomId(),
			request.fileName(),
			request.fileType()
		);

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "이미지 조회 URL 발급", description = "채팅 이미지 조회를 위한 Presigned URL을 발급합니다.")
	@GetMapping("/images/{messageId}")
	public ResponseEntity<ImageUrlResponse> getImageUrl(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable Long messageId
	) {
		Long userId = userDetails.getMemberId();

		ChatMessage message = chatMessageRepository.findById(messageId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));

		ChatRoom chatRoom = message.getChatRoom();
		if (!chatRoom.getSender().getId().equals(userId) && !chatRoom.getReceiver().getId().equals(userId)) {
			throw new CustomException(ChatErrorCode.CHAT_FORBIDDEN);
		}

		ImageUrlResponse response = chatImageService.generateDownloadUrl(message.getImageKey());

		return ResponseEntity.ok(response);
	}
}