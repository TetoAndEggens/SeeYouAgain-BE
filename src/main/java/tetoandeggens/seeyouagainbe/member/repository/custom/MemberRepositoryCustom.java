package tetoandeggens.seeyouagainbe.member.repository.custom;

import tetoandeggens.seeyouagainbe.member.entity.Member;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<Member> findByPhoneNumberIncludingDeleted(String phoneNumber);

    Optional<Member> findDeletedMemberForRestore(String loginId, String phoneNumber);
}