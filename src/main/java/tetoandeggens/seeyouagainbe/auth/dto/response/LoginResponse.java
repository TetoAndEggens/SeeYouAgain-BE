package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "LoginResponse", description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "회원 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "권한", example = "ROLE_USER")
        String role
) {}