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
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatMessageListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ChatRoomListResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.ImageUrlResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.UploadImageResponse;
import tetoandeggens.seeyouagainbe.chat.service.ChatImageService;
import tetoandeggens.seeyouagainbe.chat.service.ChatRoomService;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

@Slf4j
@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatImageService chatImageService;
	private final ChatRoomService chatRoomService;

	@Operation(summary = "내 전체 채팅방 목록 조회",
		description = "내가 참여한 모든 채팅방 목록을 조회")
	@GetMapping("/rooms")
	public ResponseEntity<ChatRoomListResponse> getMyChatRooms(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection
	) {
		ChatRoomListResponse response = chatRoomService.getMyChatRooms(
			customUserDetails.getMemberId(),
			request,
			sortDirection
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "읽지 않은 메시지가 있는 채팅방 목록 조회",
		description = "읽지 않은 메시지가 있는 채팅방 목록만 조회")
	@GetMapping("/rooms/unread")
	public ResponseEntity<ChatRoomListResponse> getUnreadChatRooms(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection
	) {
		ChatRoomListResponse response = chatRoomService.getUnreadChatRooms(
			customUserDetails.getMemberId(),
			request,
			sortDirection
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "채팅방 메시지 조회",
		description = "채팅방의 메시지를 커서 기반 페이지네이션으로 조회하고 읽지 않은 메시지를 읽음 처리")
	@GetMapping("/rooms/{chatRoomId}/messages")
	public ResponseEntity<ChatMessageListResponse> getChatMessages(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@PathVariable Long chatRoomId,
		@ParameterObject @Valid CursorPageRequest request,
		@RequestParam(defaultValue = "LATEST") SortDirection sortDirection
	) {
		ChatMessageListResponse response = chatRoomService.getChatMessages(
			chatRoomId,
			customUserDetails.getMemberId(),
			request,
			sortDirection
		);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "이미지 업로드 URL 발급", description = "채팅 이미지 업로드를 위한 Presigned URL을 발급")
	@PostMapping("/upload-url")
	public ResponseEntity<UploadImageResponse> getUploadUrl(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@Valid @RequestBody UploadImageRequest request
	) {
		UploadImageResponse response = chatImageService.generateUploadUrlWithValidation(
			customUserDetails.getMemberId(),
			request.chatRoomId(),
			request.fileName(),
			request.fileType()
		);

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "이미지 조회 URL 발급", description = "채팅 이미지 조회를 위한 Presigned URL을 발급")
	@GetMapping("/images/{messageId}")
	public ResponseEntity<ImageUrlResponse> getImageUrl(
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		@PathVariable Long messageId
	) {
		ImageUrlResponse response = chatImageService.generateDownloadUrlWithValidation(
			customUserDetails.getMemberId(),
			messageId
		);

		return ResponseEntity.ok(response);
	}
}