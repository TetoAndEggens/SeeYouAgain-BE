package tetoandeggens.seeyouagainbe.board.repository.custom;

import java.util.List;

import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardDetailResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.MyBoardResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;

public interface BoardRepositoryCustom {

	List<BoardResponse> getAnimalBoards(CursorPageRequest request, SortDirection sortDirection,
		ContentType contentType, String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town, Long memberId);

	Long getAnimalBoardsCount(ContentType contentType, String startDate, String endDate, Species species,
		String breedType, NeuteredState neuteredState, Sex sex, String city, String town);

	BoardDetailResponse getAnimalBoard(Long boardId, Long memberId);

	long countValidImageIds(List<Long> imageIds, Long animalId);

	long countValidTagIds(List<Long> tagIds, Long boardId);

	Board findByIdWithAnimal(Long boardId);

	Board findByIdWithMember(Long boardId);

	List<MyBoardResponse> getMyBoards(
		CursorPageRequest request,
		SortDirection sortDirection,
		Long memberId
	);

	Long getMyBoardsCount(Long memberId);
}