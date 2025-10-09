package tetoandeggens.seeyouagainbe.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "RegisterRequest", description = "회원가입 요청")
public record RegisterRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영어 소문자와 숫자만 사용하여 4~20자리여야 합니다.")
        @Schema(description = "로그인 아이디", example = "testuser123")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{8,16}$",
                message = "비밀번호는 8~16자리로 영문 대소문자, 숫자, 특수문자를 포함해야 합니다.")
        @Schema(description = "비밀번호", example = "Password123!")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,10}$", message = "닉네임은 한글, 영문, 숫자 2~10자리여야 합니다.")
        @Schema(description = "닉네임", example = "테스트유저")
        String nickName,

        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        @Pattern(regexp = "^010\\d{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다.")
        @Schema(description = "휴대전화 번호", example = "01012345678")
        String phoneNumber
) {
}
