package tetoandeggens.seeyouagainbe.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.chat.dto.request.UploadImageRequest;
import tetoandeggens.seeyouagainbe.chat.dto.response.ImageUrlResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.UploadImageResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.chat.service.ChatImageService;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;

@Slf4j
@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatImageService chatImageService;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;

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

		// Presigned URL 생성
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

		// 메시지 조회
		ChatMessage message = chatMessageRepository.findById(messageId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));

		// 권한 검증
		ChatRoom chatRoom = message.getChatRoom();
		if (!chatRoom.getSender().getId().equals(userId) && !chatRoom.getReceiver().getId().equals(userId)) {
			throw new CustomException(ChatErrorCode.CHAT_FORBIDDEN);
		}

		// Presigned URL 생성
		ImageUrlResponse response = chatImageService.generateDownloadUrl(message.getImageKey());

		return ResponseEntity.ok(response);
	}
}