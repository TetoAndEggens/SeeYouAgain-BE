package tetoandeggens.seeyouagainbe.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UploadImageRequest: 이미지 업로드 URL 요청 Dto")
public record UploadImageRequest(
	@NotBlank(message = "파일명은 필수입니다.")
	@Schema(description = "파일명", example = "macbook_photo.jpg")
	String fileName,

	@NotBlank(message = "파일 타입은 필수입니다.")
	@Schema(description = "파일 MIME 타입", example = "image/jpeg")
	String fileType,

	@NotNull(message = "채팅방 ID는 필수입니다.")
	@Schema(description = "채팅방 ID", example = "123")
	Long chatRoomId,

	@NotNull(message = "게시물 ID는 필수입니다.")
	@Schema(description = "게시물 ID", example = "999")
	Long boardId,

	@NotNull(message = "수신자 ID는 필수입니다.")
	@Schema(description = "수신자 ID", example = "2")
	Long receiverId
) {
}