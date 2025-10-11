package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.request.RegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.entity.SocialType;
import tetoandeggens.seeyouagainbe.domain.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (memberRepository.existsByLoginIdAndIsDeletedFalse(registerRequest.loginId())) {
            throw new CustomException(AuthErrorCode.DUPLICATED_LOGIN_ID);
        }

        if (memberRepository.existsByPhoneNumberAndIsDeletedFalse(registerRequest.phoneNumber())) {
            throw new CustomException(AuthErrorCode.DUPLICATED_PHONE_NUMBER);
        }

        String encodedPassword = passwordEncoder.encode(registerRequest.password());

        Member member = Member.builder()
                .loginId(registerRequest.loginId())
                .password(encodedPassword)
                .nickName(registerRequest.nickName())
                .phoneNumber(registerRequest.phoneNumber())
                .type(SocialType.GENERAL)
                .build();

        memberRepository.save(member);
    }

    public ReissueTokenResponse reissueToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = tokenProvider.resolveRefreshToken(request);

        if (refreshToken == null) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        String newAccessToken = tokenProvider.reissueAccessToken(refreshToken);

        return ReissueTokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    public void checkLoginIdAvailable(String loginId) {
        if (memberRepository.existsByLoginIdAndIsDeletedFalse(loginId)) {
            throw new CustomException(AuthErrorCode.DUPLICATED_LOGIN_ID);
        }
    }
}
