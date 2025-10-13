package tetoandeggens.seeyouagainbe.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(name = "PhoneVerificationResultResponse", description = "휴대폰 인증 정보 응답 DTO")
public record PhoneVerificationResultResponse(
        @Schema(description = "인증 번호", example = "123456")
        String code,

        @Email
        @Schema(description = "서버 이메일 주소", example = "taetoeggen556@gmail.com")
        String emailAddress
) {}