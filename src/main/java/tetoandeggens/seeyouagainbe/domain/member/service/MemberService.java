package tetoandeggens.seeyouagainbe.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginIdAndIsDeletedFalse(loginId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    public Member findByPhoneNumber(String phoneNumber) {
        return memberRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    public Member findByUuid(String uuid) {
        return memberRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    public Member findById(Long memberId) {
        return memberRepository.findByIdAndIsDeletedFalse(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void softDeleteMember(Long memberId) {
        Member member = findById(memberId);
        member.updateDeleteStatus();
    }
}
