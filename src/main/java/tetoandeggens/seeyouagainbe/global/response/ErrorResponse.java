package tetoandeggens.seeyouagainbe.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ErrorCode;

@Getter
@Builder
@Schema(name = "ErrorResponse", description = "에러 응답")
public class ErrorResponse {
    @Schema(description = "에러 코드", example = "AUTH_001")
    private final String code;

    @Schema(description = "에러 메시지", example = "인증에 실패했습니다.")
    private final String message;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}