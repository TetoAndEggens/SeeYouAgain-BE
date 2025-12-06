package tetoandeggens.seeyouagainbe.animal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;

@Builder
@Schema(name = "BookMarkResponse: 북마크 토글 응답 Dto")
public record BookMarkResponse(
        @Schema(description = "북마크 고유 ID", example = "1")
        Long bookMarkId,

        @Schema(description = "북마크 여부", example = "true")
        Boolean isBookMarked
) {
    public static BookMarkResponse from(BookMark bookMark) {
        return BookMarkResponse.builder()
                .bookMarkId(bookMark.getId())
                .isBookMarked(!bookMark.getIsDeleted())
                .build();
    }
}