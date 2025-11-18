package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CheckUserIdRequest", description = "아이디 중복검증 요청 DTO")
public record CheckUserIdRequest(
        @NotBlank(message = "loginId는 비어 있을 수 없습니다.")
        String loginId
) {}