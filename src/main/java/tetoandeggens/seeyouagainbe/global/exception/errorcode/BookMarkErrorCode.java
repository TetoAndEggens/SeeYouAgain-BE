package tetoandeggens.seeyouagainbe.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookMarkErrorCode implements ErrorCode {

    ONLY_ABANDONED_ANIMAL_CAN_BE_BOOKMARKED("BOOKMARK_001","유기 동물만 북마크할 수 있습니다.",HttpStatus.BAD_REQUEST),
    BOOKMARK_NOT_FOUND("BOOKMARK_002","북마크를 찾을 수 없습니다.",HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
