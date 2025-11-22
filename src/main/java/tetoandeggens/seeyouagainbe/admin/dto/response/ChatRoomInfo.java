package tetoandeggens.seeyouagainbe.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ChatRoomInfo", description = "채팅방 정보")
public record ChatRoomInfo(
        @Schema(description = "채팅방 ID", example = "1")
        Long chatRoomId,

        @Schema(description = "생성일", example = "2025-01-15T10:30:00")
        LocalDateTime createdAt
) {}