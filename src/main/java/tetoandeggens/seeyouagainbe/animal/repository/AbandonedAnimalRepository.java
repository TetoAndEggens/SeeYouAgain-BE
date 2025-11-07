package tetoandeggens.seeyouagainbe.animal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.animal.entity.AbandonedAnimal;
import tetoandeggens.seeyouagainbe.animal.repository.custom.AbandonedAnimalRepositoryCustom;

public interface AbandonedAnimalRepository extends JpaRepository<AbandonedAnimal, Long>,
	AbandonedAnimalRepositoryCustom {
}
