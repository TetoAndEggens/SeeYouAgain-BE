package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationKeywordErrorCode implements ErrorCode {

    KEYWORD_NOT_FOUND("KEYWORD-001", "구독 중인 키워드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    KEYWORD_ALREADY_SUBSCRIBED("KEYWORD-002", "이미 구독 중인 키워드입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
