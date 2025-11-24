package tetoandeggens.seeyouagainbe.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "UploadImageResponse: 이미지 업로드 URL 응답 Dto")
public record UploadImageResponse(
	@Schema(description = "Presigned Upload URL", example = "https://s3.amazonaws.com/...")
	String url,

	@Schema(description = "이미지 S3 Key", example = "chat-images/123/abc-def-123_macbook_photo.jpg")
	String imageS3Key
) {
}
