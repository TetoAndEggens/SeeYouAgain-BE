package tetoandeggens.seeyouagainbe.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.MemberErrorCode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public Member getMemberByUuid(String uuid) {
        return memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public void updateNickName(Long memberId, String newNickName) {
        Member member = getMemberById(memberId);
        member.updateNickName(newNickName);
    }

    @Transactional
    public void updateProfile(Long memberId, String profileUrl) {
        Member member = getMemberById(memberId);
        member.updateProfile(profileUrl);
    }

    @Transactional
    public void togglePushNotification(Long memberId) {
        Member member = getMemberById(memberId);
        member.togglePushNotification();
    }
}
