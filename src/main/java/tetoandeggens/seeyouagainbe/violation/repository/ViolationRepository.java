package tetoandeggens.seeyouagainbe.violation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.custom.ViolationRepositoryCustom;

import java.util.Optional;

public interface ViolationRepository extends JpaRepository<Violation, Long>,
        ViolationRepositoryCustom {

    // 신고 상세 조회 또는 처리 시 사용
    @Query("SELECT DISTINCT v FROM Violation v "
            + "JOIN FETCH v.reporter r "
            + "JOIN FETCH v.reportedMember m "
            + "LEFT JOIN FETCH v.board b "
            + "LEFT JOIN FETCH v.chatRoom c "
            + "WHERE v.id = :violationId")
    Optional<Violation> findByIdWithAllFetch(@Param("violationId") Long violationId);
}