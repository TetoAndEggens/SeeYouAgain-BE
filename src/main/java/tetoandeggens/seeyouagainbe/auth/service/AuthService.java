package tetoandeggens.seeyouagainbe.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.request.ReissueTokenRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.MemberErrorCode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public void register(RegisterRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new CustomException(MemberErrorCode.DUPLICATE_LOGIN_ID);
        }

        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new CustomException(MemberErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.createLocal(
                request.loginId(),
                encodedPassword,
                request.nickName(),
                request.phoneNumber()
        );

        memberRepository.save(member);
    }

    public ReissueTokenResponse reissueToken(ReissueTokenRequest request) {
        try {
            String newAccessToken = tokenProvider.reissueAccessToken(request.refreshToken());
            return new ReissueTokenResponse(newAccessToken);
        } catch (IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        tokenProvider.deleteRefreshToken(String.valueOf(memberId));
        memberRepository.delete(member);
    }

    public boolean checkLoginIdAvailable(String loginId) {
        return !memberRepository.existsByLoginId(loginId);
    }

    public boolean checkPhoneNumberAvailable(String phoneNumber) {
        return !memberRepository.existsByPhoneNumber(phoneNumber);
    }
}
