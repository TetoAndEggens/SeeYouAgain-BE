package tetoandeggens.seeyouagainbe.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.member.dto.request.UpdatePushEnabledRequest;
import tetoandeggens.seeyouagainbe.member.dto.response.UpdatePushEnabledResponse;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Member findById(Long memberId) {
        return memberRepository.findByIdAndIsDeletedFalse(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public UpdatePushEnabledResponse updatePushEnabled(Long memberId, UpdatePushEnabledRequest request) {
        Member member = memberRepository.findByIdAndIsDeletedFalse(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));

        member.updatePushEnabled(request.isPushEnabled());

        return UpdatePushEnabledResponse.from(member.getIsPushEnabled());
    }
}
