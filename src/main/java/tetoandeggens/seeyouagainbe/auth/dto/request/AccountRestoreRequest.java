package tetoandeggens.seeyouagainbe.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountRestoreRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다")
        String loginId,

        @NotBlank(message = "휴대폰 번호는 필수입니다")
        @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다.")
        String phoneNumber
) {}