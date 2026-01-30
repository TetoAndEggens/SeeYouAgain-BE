package tetoandeggens.seeyouagainbe.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;

@Schema(name = "MyBoardListResponse", description = "내가 작성한 게시글 리스트 응답 DTO")
public record MyBoardListResponse(
    @Schema(description = "내가 작성한 총 게시글 수", example = "12")
    int boardCount,

    CursorPage<MyBoardResponse, Long> board
) {
    public static MyBoardListResponse of(int boardCount, CursorPage<MyBoardResponse, Long> board) {
        return new MyBoardListResponse(boardCount, board);
    }
}