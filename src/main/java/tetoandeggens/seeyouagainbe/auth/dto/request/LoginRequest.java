package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "로그인 요청")
public record LoginRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Schema(description = "로그인 아이디", example = "testuser123")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호", example = "Password123!")
        String password
) {}