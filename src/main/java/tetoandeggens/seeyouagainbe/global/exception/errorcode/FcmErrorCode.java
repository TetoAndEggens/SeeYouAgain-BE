package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FcmErrorCode implements ErrorCode {

    // FCM 토큰 관련
    TOKEN_NOT_FOUND("FCM-001", "FCM 토큰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    TOKEN_ALREADY_EXISTS("FCM-002", "이미 등록된 FCM 토큰입니다.", HttpStatus.CONFLICT),
    TOKEN_EXPIRED("FCM-003", "만료된 FCM 토큰입니다.", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN("FCM-004", "유효하지 않은 FCM 토큰입니다.", HttpStatus.BAD_REQUEST),

    // Firebase 연동 관련
    FIREBASE_SUBSCRIBE_FAILED("FCM-005", "Firebase Topic 구독에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FIREBASE_UNSUBSCRIBE_FAILED("FCM-006", "Firebase Topic 구독 해제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FIREBASE_MESSAGING_FAILED("FCM-007", "Firebase 메시지 전송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 키워드 관련
    KEYWORD_NOT_FOUND("FCM-008", "구독 중인 키워드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    KEYWORD_ALREADY_SUBSCRIBED("FCM-009", "이미 구독 중인 키워드입니다.", HttpStatus.CONFLICT),
    PUSH_DISABLED("FCM-010", "푸시 알림이 비활성화되어 있습니다.", HttpStatus.FORBIDDEN),

    // 회원 관련
    MEMBER_NOT_FOUND("FCM-011", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND),

    // 기타
    INTERNAL_ERROR("FCM-012", "FCM 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}