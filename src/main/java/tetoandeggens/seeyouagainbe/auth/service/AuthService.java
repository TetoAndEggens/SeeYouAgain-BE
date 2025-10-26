package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.request.UnifiedRegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.util.GeneratorRandomUtil;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

import java.time.Duration;
import java.time.LocalDateTime;

import static tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    @Transactional
    public void unifiedRegister(UnifiedRegisterRequest request) {
        String verifiedKey = request.hasSocialInfo()
                ? PREFIX_SOCIAL_VERIFIED + request.phoneNumber()
                : request.phoneNumber();

        String isVerified = redisTemplate.opsForValue().get(verifiedKey);

        if (isVerified == null || !isVerified.equals(VERIFIED)) {
            throw new CustomException(AuthErrorCode.PHONE_NOT_VERIFIED);
        }

        checkLoginIdAvailable(request.loginId());

        String finalSocialId = request.socialId();
        String finalProfileImageUrl = request.profileImageUrl();
        String finalProvider = request.socialProvider();

        if (request.hasSocialInfo() && finalSocialId == null) {
            finalProvider = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROVIDER + request.phoneNumber());
            finalSocialId = redisTemplate.opsForValue().get(PREFIX_SOCIAL_ID + request.phoneNumber());
            finalProfileImageUrl = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROFILE + request.phoneNumber());
        }

        Member member = Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .nickName(request.nickName())
                .phoneNumber(request.phoneNumber())
                .profile(finalProfileImageUrl)
                .socialIdKakao("kakao".equals(finalProvider) ? finalSocialId : null)
                .socialIdNaver("naver".equals(finalProvider) ? finalSocialId : null)
                .socialIdGoogle("google".equals(finalProvider) ? finalSocialId : null)
                .build();

        memberRepository.save(member);

        if (request.hasSocialInfo()) {
            redisTemplate.delete(PREFIX_SOCIAL_VERIFIED + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_PROVIDER + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_ID + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_PROFILE + request.phoneNumber());
        } else {
            redisTemplate.delete(request.phoneNumber());
        }
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

    public void checkPhoneNumberDuplicate(String phoneNumber) {
        if (memberRepository.existsByPhoneNumberAndIsDeletedFalse(phoneNumber)) {
            throw new CustomException(AuthErrorCode.PHONE_NUMBER_DUPLICATED);
        }
    }

    @Transactional
    public PhoneVerificationResultResponse sendPhoneVerificationCode(String phone) {
        checkPhoneNumberDuplicate(phone);

        String code = GeneratorRandomUtil.generateRandomNum();
        LocalDateTime now = LocalDateTime.now();

        redisTemplate.opsForValue().set(PREFIX_VERIFICATION_CODE + phone, code, Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(PREFIX_VERIFICATION_TIME + phone, now.toString(), Duration.ofMinutes(VERIFICATION_TIME));

        String emailAddress = emailService.getServerEmail();
        return new PhoneVerificationResultResponse(code, emailAddress);
    }

    @Transactional
    public void verifyPhoneCode(String phone) {
        String code = redisTemplate.opsForValue().get(PREFIX_VERIFICATION_CODE + phone);
        String time = redisTemplate.opsForValue().get(PREFIX_VERIFICATION_TIME + phone);

        if (code == null || time == null) {
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        LocalDateTime createdAt = LocalDateTime.parse(time);
        boolean result = emailService.extractCodeByPhoneNumber(code, phone, createdAt);

        if (!result) {
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        redisTemplate.delete(PREFIX_VERIFICATION_CODE + phone);
        redisTemplate.delete(PREFIX_VERIFICATION_TIME + phone);

        redisTemplate.opsForValue().set(phone, VERIFIED, Duration.ofMinutes(VERIFICATION_TIME));
    }
}
