package tetoandeggens.seeyouagainbe.violation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.custom.ViolationRepositoryCustom;

public interface ViolationRepository extends JpaRepository<Violation, Long>,
        ViolationRepositoryCustom {
}