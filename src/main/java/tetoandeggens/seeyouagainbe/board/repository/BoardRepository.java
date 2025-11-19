package tetoandeggens.seeyouagainbe.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.custom.BoardRepositoryCustom;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {
}
