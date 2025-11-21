package tetoandeggens.seeyouagainbe.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.custom.BoardRepositoryCustom;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {

	@Modifying
	@Query("update AnimalS3Profile p set p.isDeleted = true where p.animal.id = :animalId")
	void softDeleteByAnimalId(Long animalId);

	@Modifying
	@Query("update AnimalS3Profile p set p.isDeleted = true where p.id in :imageIds")
	void softDeleteByImageIds(List<Long> imageIds);
}
