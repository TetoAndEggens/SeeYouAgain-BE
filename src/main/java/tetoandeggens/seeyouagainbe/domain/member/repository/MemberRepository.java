package tetoandeggens.seeyouagainbe.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.entity.SocialType;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findBySocialIdAndType(String socialId, SocialType type);

    Optional<Member> findByUuid(String uuid);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    boolean existsByLoginId(String loginId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsBySocialIdAndType(String socialId, SocialType type);
}
