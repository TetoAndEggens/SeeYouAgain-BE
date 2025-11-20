package tetoandeggens.seeyouagainbe.board.repository.custom;

import static tetoandeggens.seeyouagainbe.board.entity.QBoardTag.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.BoardTag;

@Repository
@RequiredArgsConstructor
public class BoardTagRepositoryCustomImpl implements BoardTagRepositoryCustom {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final JPAQueryFactory queryFactory;

	@Override
	public void bulkInsert(List<String> tags, Board board) {
		String sql = "INSERT INTO board_tag (board_id, name, created_at, updated_at) "
			+ "VALUES (:boardId, :name, :createdAt, :updatedAt)";

		LocalDateTime now = LocalDateTime.now();

		SqlParameterSource[] batchParams = tags.stream()
			.map(tag -> new MapSqlParameterSource()
				.addValue("boardId", board.getId())
				.addValue("name", tag)
				.addValue("createdAt", now)
				.addValue("updatedAt", now))
			.toArray(SqlParameterSource[]::new);

		namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
	}

	@Override
	public List<BoardTag> findByBoardIdInWithBoard(List<Long> boardIds) {
		return queryFactory
			.selectFrom(boardTag)
			.join(boardTag.board).fetchJoin()
			.where(boardTag.board.id.in(boardIds))
			.fetch();
	}
}