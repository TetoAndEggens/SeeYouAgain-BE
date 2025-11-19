package tetoandeggens.seeyouagainbe.animal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbe.animal.entity.AnimalLocation;

public interface AnimalLocationRepository extends JpaRepository<AnimalLocation, Long> {
}