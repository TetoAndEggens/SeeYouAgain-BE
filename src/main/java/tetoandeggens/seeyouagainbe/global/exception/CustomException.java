package tetoandeggens.seeyouagainbe.global.exception;

import lombok.Getter;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ErrorCode;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}