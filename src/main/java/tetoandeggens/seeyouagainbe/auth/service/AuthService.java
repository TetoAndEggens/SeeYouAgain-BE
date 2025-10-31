package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.request.UnifiedRegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.request.WithdrawalRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.ReissueTokenResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.provider.OAuth2UnlinkProvider;
import tetoandeggens.seeyouagainbe.auth.util.GeneratorRandomUtil;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final OAuth2UnlinkProvider oAuth2UnlinkProvider;
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
        String finalRefreshToken = null;

        if (request.socialProvider() != null && request.socialId() == null) {
            String provider = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROVIDER + request.phoneNumber());
            String socialId = redisTemplate.opsForValue().get(PREFIX_SOCIAL_ID + request.phoneNumber());
            String profileImageUrl = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROFILE + request.phoneNumber());
            String refreshToken = redisTemplate.opsForValue().get(PREFIX_SOCIAL_REFRESH_TOKEN + request.phoneNumber());

            if (provider == null || socialId == null) {
                throw new CustomException(AuthErrorCode.REAUTH_TOKEN_NOT_FOUND);
            }

            finalProvider = provider;
            finalSocialId = socialId;
            finalProfileImageUrl = profileImageUrl;
            finalRefreshToken = refreshToken;
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

        // RefreshToken 저장 (네이버/구글만)
        if ("naver".equals(finalProvider) && finalRefreshToken != null) {
            member.updateNaverRefreshToken(finalRefreshToken);
        } else if ("google".equals(finalProvider) && finalRefreshToken != null) {
            member.updateGoogleRefreshToken(finalRefreshToken);
        }

        memberRepository.save(member);

        // Redis 정리
        if (request.hasSocialInfo()) {
            redisTemplate.delete(PREFIX_SOCIAL_VERIFIED + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_PROVIDER + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_ID + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_PROFILE + request.phoneNumber());
            redisTemplate.delete(PREFIX_SOCIAL_REFRESH_TOKEN + request.phoneNumber());
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

    @Transactional
    public void withdrawMember(String uuid, WithdrawalRequest request) {
        log.info("[Withdrawal] 회원 탈퇴 시작 - uuid: {}", uuid);

        Member member = memberRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(AuthErrorCode.WRONG_ID_PW);
        }

        // 소셜 연동 해제 (RefreshToken 사용)
        unlinkAllSocialAccounts(member);

        // Redis 데이터 삭제
        clearRedisData(uuid);

        // Soft Delete
        member.updateDeleteStatus();
        memberRepository.save(member);

        log.info("[Withdrawal] 회원 탈퇴 완료 - memberId: {}", member.getId());
    }

    private void unlinkAllSocialAccounts(Member member) {
        List<String> failedUnlinks = new ArrayList<>();

        // 1. 카카오 연동 해제 (Admin Key 사용)
        if (member.getSocialIdKakao() != null) {
            try {
                boolean success = oAuth2UnlinkProvider.unlinkKakao(member.getSocialIdKakao());

                if (success) {
                    member.deleteKakaoSocialId();
                    log.info("[Withdrawal] 카카오 연동 해제 성공");
                } else {
                    failedUnlinks.add("kakao");
                }
            } catch (Exception e) {
                log.error("[Withdrawal] 카카오 연동 해제 중 오류", e);
                member.deleteKakaoSocialId(); // DB에서는 제거
            }
        }

        // 2. 네이버 연동 해제 (RefreshToken 사용)
        if (member.getSocialIdNaver() != null) {
            try {
                boolean success = oAuth2UnlinkProvider.unlinkNaver(member);

                if (success) {
                    member.deleteNaverSocialId();
                    member.deleteNaverRefreshToken();
                    log.info("[Withdrawal] 네이버 연동 해제 성공");
                } else {
                    log.warn("[Withdrawal] 네이버 연동 해제 실패 - DB에서만 제거");
                    member.deleteNaverSocialId();
                    member.deleteNaverRefreshToken();
                }
            } catch (Exception e) {
                log.error("[Withdrawal] 네이버 연동 해제 중 오류", e);
                member.deleteNaverSocialId();
                member.deleteNaverRefreshToken();
            }
        }

        // 3. 구글 연동 해제 (RefreshToken 사용)
        if (member.getSocialIdGoogle() != null) {
            try {
                boolean success = oAuth2UnlinkProvider.unlinkGoogle(member);

                if (success) {
                    member.deleteGoogleSocialId();
                    member.deleteGoogleRefreshToken();
                    log.info("[Withdrawal] 구글 연동 해제 성공");
                } else {
                    log.warn("[Withdrawal] 구글 연동 해제 실패 - DB에서만 제거");
                    member.deleteGoogleSocialId();
                    member.deleteGoogleRefreshToken();
                }
            } catch (Exception e) {
                log.error("[Withdrawal] 구글 연동 해제 중 오류", e);
                member.deleteGoogleSocialId();
                member.deleteGoogleRefreshToken();
            }
        }

        if (!failedUnlinks.isEmpty()) {
            log.warn("[Withdrawal] 일부 소셜 연동 해제 실패: {}", failedUnlinks);
        }
    }

    private void clearRedisData(String uuid) {
        try {
            redisTemplate.delete(uuid);
        } catch (Exception e) {
            log.error("[Withdrawal] Redis 데이터 삭제 중 오류", e);
        }
    }
}