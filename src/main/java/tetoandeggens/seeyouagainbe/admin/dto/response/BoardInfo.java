package tetoandeggens.seeyouagainbe.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "BoardInfo", description = "게시물 정보")
public record BoardInfo(
        @Schema(description = "게시물 ID", example = "1")
        Long boardId,

        @Schema(description = "제목", example = "게시물 제목")
        String title,

        @Schema(description = "내용", example = "게시물 내용")
        String content,

        @Schema(description = "작성일", example = "2025-01-15T10:30:00")
        LocalDateTime createdAt
) {}