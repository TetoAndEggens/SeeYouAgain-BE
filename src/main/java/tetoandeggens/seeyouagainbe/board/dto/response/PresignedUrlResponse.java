package tetoandeggens.seeyouagainbe.board.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AnimalBoardResponse: 게시물 작성 응답 Dto")
public record PresignedUrlResponse(
	@Schema(description = "Presigned URL 리스트", example = "[\"https://s3.amazonaws.com/...\"]")
	List<String> presignedUrls
) {
}