package tetoandeggens.seeyouagainbe.animal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.repository.custom.AnimalRepositoryCustom;

public interface AnimalRepository extends JpaRepository<Animal, Long>,
	AnimalRepositoryCustom {
}