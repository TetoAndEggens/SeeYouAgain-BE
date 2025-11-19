package tetoandeggens.seeyouagainbe.board.repository.custom;

import java.util.List;

import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;

public interface BoardRepositoryCustom {

	List<BoardResponse> getAnimalBoards(CursorPageRequest request, SortDirection sortDirection,
		ContentType contentType);

	Long getAnimalBoardsCount(ContentType contentType);
}