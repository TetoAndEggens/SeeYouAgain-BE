package tetoandeggens.seeyouagainbe.member.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.Optional;

import static tetoandeggens.seeyouagainbe.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Member> findByPhoneNumberIncludingDeleted(String phoneNumber) {
        Member result = queryFactory
                .selectFrom(member)
                .where(member.phoneNumber.eq(phoneNumber))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Member> findDeletedMemberForRestore(String loginId, String phoneNumber) {
        Member result = queryFactory
                .selectFrom(member)
                .where(
                        member.loginId.eq(loginId),
                        member.phoneNumber.eq(phoneNumber),
                        member.isDeleted.eq(true)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}