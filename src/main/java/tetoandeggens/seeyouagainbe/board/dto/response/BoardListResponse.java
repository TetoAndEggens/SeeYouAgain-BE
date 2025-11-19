package tetoandeggens.seeyouagainbe.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tetoandeggens.seeyouagainbe.common.dto.CursorPage;

@Schema(name = "BoardListResponse: 게시글 리스트 응답 Dto")
public record BoardListResponse(
	@Schema(description = "총 게시글 수", example = "24")
	int boardCount,

	CursorPage<BoardResponse, Long> board
) {
	public static BoardListResponse of(int boardCount,
		CursorPage<BoardResponse, Long> board) {
		return new BoardListResponse(boardCount, board);
	}
}