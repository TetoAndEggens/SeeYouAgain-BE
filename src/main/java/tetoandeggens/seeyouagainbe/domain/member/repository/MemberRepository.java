package tetoandeggens.seeyouagainbe.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginIdAndIsDeletedFalse(String loginId);

    Optional<Member> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    Optional<Member> findByIdAndIsDeletedFalse(Long id);

    Optional<Member> findByUuidAndIsDeletedFalse(String uuid);

    boolean existsByLoginIdAndIsDeletedFalse(String loginId);

    boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);
}
