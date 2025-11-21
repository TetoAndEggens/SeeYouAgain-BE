package tetoandeggens.seeyouagainbe.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import tetoandeggens.seeyouagainbe.board.entity.BoardTag;
import tetoandeggens.seeyouagainbe.board.repository.custom.BoardTagRepositoryCustom;

public interface BoardTagRepository extends JpaRepository<BoardTag, Long>, BoardTagRepositoryCustom {

	@Modifying
	@Query("update BoardTag bt set bt.isDeleted = true where bt.id in :tagIds")
	void softDeleteByTagIds(java.util.List<Long> tagIds);
}