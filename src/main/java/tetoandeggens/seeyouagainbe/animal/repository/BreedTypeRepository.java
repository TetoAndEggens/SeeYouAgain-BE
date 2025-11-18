package tetoandeggens.seeyouagainbe.animal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.animal.entity.BreedType;

public interface BreedTypeRepository extends JpaRepository<BreedType, Long> {
	Optional<BreedType> findByName(String name);
}