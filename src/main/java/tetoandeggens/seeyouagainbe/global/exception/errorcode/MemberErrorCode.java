package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND("MEMBER_001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_DELETED("MEMBER_002", "이미 탈퇴한 회원입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}