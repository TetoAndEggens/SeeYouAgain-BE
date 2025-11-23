package tetoandeggens.seeyouagainbe.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "ImageUrlResponse: 이미지 조회 URL 응답 Dto")
public record ImageUrlResponse(
	@Schema(description = "Presigned GET URL", example = "https://s3.amazonaws.com/...")
	String url
) {
}