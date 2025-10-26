package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "SocialLoginResultResponse", description = "소셜 로그인 결과 응답")
public record SocialLoginResultResponse(
        @Schema(description = "상태 코드 (LOGIN: 즉시 로그인, LINK: 계정 연동 필요, SIGNUP: 회원가입 필요)",
                example = "LOGIN")
        String status,

        @Schema(description = "상태 메시지", example = "로그인 성공")
        String message,

        @Schema(description = "로그인 응답 (status가 LOGIN일 때만 포함)")
        LoginResponse loginResponse
) {}