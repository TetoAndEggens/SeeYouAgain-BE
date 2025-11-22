package tetoandeggens.seeyouagainbe.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MemberInfo", description = "회원 정보")
public record MemberInfo(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "닉네임", example = "사용자1")
        String nickName,

        @Schema(description = "로그인 ID", example = "user@example.com")
        String loginId,

        @Schema(description = "위반 횟수", example = "2")
        Long violatedCount,

        @Schema(description = "정지 여부", example = "false")
        Boolean isBanned
) {}