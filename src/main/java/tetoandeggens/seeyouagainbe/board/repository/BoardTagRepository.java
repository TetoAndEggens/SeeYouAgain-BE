package tetoandeggens.seeyouagainbe.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.board.entity.BoardTag;
import tetoandeggens.seeyouagainbe.board.repository.custom.BoardTagRepositoryCustom;

public interface BoardTagRepository extends JpaRepository<BoardTag, Long>, BoardTagRepositoryCustom {
}