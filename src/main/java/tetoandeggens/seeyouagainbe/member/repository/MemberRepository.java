package tetoandeggens.seeyouagainbe.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.custom.MemberRepositoryCustom;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Optional<Member> findByLoginIdAndIsDeletedFalse(String loginId);

    Optional<Member> findByIdAndIsDeletedFalse(Long id);

    Optional<Member> findByUuidAndIsDeletedFalse(String uuid);

    boolean existsByLoginIdAndIsDeletedFalse(String loginId);

    boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    boolean existsByIdAndIsPushEnabled(Long id, boolean isPushEnabled);

    Optional<Member> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    Optional<Member> findBySocialIdKakaoAndIsDeletedFalse(String socialIdKakao);

    Optional<Member> findBySocialIdNaverAndIsDeletedFalse(String socialIdNaver);

    Optional<Member> findBySocialIdGoogleAndIsDeletedFalse(String socialIdGoogle);
}
