package tetoandeggens.seeyouagainbe.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;

@Schema(name = "ViolationProcessRequest", description = "신고 처리 요청 DTO")
public record ViolationProcessRequest(
        @NotNull(message = "처리 상태는 필수입니다.")
        @Schema(description = "처리 상태 (VIOLATED: 위반, NORMAL: 위반 아님)", example = "VIOLATED")
        ViolatedStatus violatedStatus,

        @Schema(description = "콘텐츠 삭제 여부 (기본값: 위반 처리 시 true, 위반 아님 처리 시 false)", example = "true")
        Boolean deleteContent
) {
}