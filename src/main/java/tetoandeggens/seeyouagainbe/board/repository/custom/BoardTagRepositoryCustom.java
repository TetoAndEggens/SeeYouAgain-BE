package tetoandeggens.seeyouagainbe.board.repository.custom;

import java.util.List;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.BoardTag;

public interface BoardTagRepositoryCustom {

	void bulkInsert(List<String> tags, Board board);

	List<BoardTag> findByBoardIdInWithBoard(List<Long> boardIds);
}