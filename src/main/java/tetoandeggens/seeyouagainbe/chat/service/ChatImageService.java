package tetoandeggens.seeyouagainbe.chat.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.chat.dto.response.ImageUrlResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.UploadImageResponse;
import tetoandeggens.seeyouagainbe.chat.entity.ChatMessage;
import tetoandeggens.seeyouagainbe.chat.repository.ChatMessageRepository;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;
import tetoandeggens.seeyouagainbe.image.service.ImageService;

@Service
@RequiredArgsConstructor
public class ChatImageService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ImageService imageService;

	private static final String S3_OBJECT_KEY_FORMAT = "chat-images/%d/%s_%s";

	@Transactional
	public UploadImageResponse generateUploadUrlWithValidation(Long memberId, Long chatRoomId, String fileName,
		String fileType) {
		chatRoomRepository.findByIdWithMembersAndValidateAccess(chatRoomId, memberId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_FORBIDDEN));

		return generateUploadUrl(chatRoomId, fileName, fileType);
	}

	@Transactional(readOnly = true)
	public ImageUrlResponse generateDownloadUrlWithValidation(Long memberId, Long messageId) {
		ChatMessage message = chatMessageRepository.findByIdWithChatRoomAndMembersAndValidateAccess(messageId, memberId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_FORBIDDEN));

		return generateDownloadUrl(message.getImageKey());
	}

	private UploadImageResponse generateUploadUrl(Long chatRoomId, String fileName, String fileType) {
		String uuid = UUID.randomUUID().toString();
		String objectKey = String.format(S3_OBJECT_KEY_FORMAT, chatRoomId, uuid, fileName);

		String uploadUrl = imageService.generateUploadPresignedUrl(objectKey, fileType, Duration.ofMinutes(5));

		return UploadImageResponse.builder()
			.url(uploadUrl)
			.imageS3Key(objectKey)
			.build();
	}

	private ImageUrlResponse generateDownloadUrl(String imageKey) {
		String downloadUrl = imageService.generateDownloadPresignedUrl(imageKey, Duration.ofHours(1));

		return ImageUrlResponse.builder()
			.url(downloadUrl)
			.build();
	}
}