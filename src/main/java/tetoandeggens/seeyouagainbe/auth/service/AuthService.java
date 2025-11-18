package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.request.UnifiedRegisterRequest;
import tetoandeggens.seeyouagainbe.auth.dto.request.WithdrawalRequest;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2UnlinkServiceProvider;
import tetoandeggens.seeyouagainbe.auth.util.GeneratorRandomUtil;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

import java.time.LocalDateTime;
import java.util.Map;

import static tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RedisAuthService redisAuthService;
    private final CookieService cookieService;
    private final EmailService emailService;
    private final SocialAccountLinkStrategy socialAccountLinkStrategy;
    private final Map<String, OAuth2UnlinkServiceProvider> unlinkServices; // Provider별 Unlink Service를 Map으로 주입

    @Transactional
    public void unifiedRegister(UnifiedRegisterRequest request, HttpServletResponse response) {
        validatePhoneVerification(request);
        checkLoginIdAvailable(request.loginId());

        SocialInfo socialInfo = extractSocialInfo(request);

        Member member = buildMember(request, socialInfo);
        memberRepository.save(member);

        if (socialInfo.provider() != null && socialInfo.refreshToken() != null) {
            cookieService.deleteTempTokenCookie(response);
        }

        cleanupRedisAfterRegister(request, socialInfo);
    }


    public void reissueToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.resolveRefreshToken(request);

        if (refreshToken == null) {
            throw new CustomException(REFRESH_TOKEN_NOT_FOUND);
        }

        tokenProvider.validateToken(refreshToken);

        String uuid = tokenProvider.parseClaims(refreshToken).getSubject();
        String role = tokenProvider.parseClaims(refreshToken).get("role", String.class);

        String storedRefreshToken = redisAuthService.getRefreshToken(uuid)
                .orElseThrow(() -> new CustomException(REFRESH_TOKEN_NOT_FOUND));

        if (!storedRefreshToken.equals(refreshToken)) {
            throw new CustomException(REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = tokenProvider.createAccessToken(uuid, role);
        cookieService.setAccessTokenCookie(
                response,
                newAccessToken,
                tokenProvider.getAccessTokenExpirationSec()
        );
    }

    public void checkLoginIdAvailable(String loginId) {
        if (memberRepository.existsByLoginIdAndIsDeletedFalse(loginId)) {
            throw new CustomException(DUPLICATED_LOGIN_ID);
        }
    }

    public void checkPhoneNumberDuplicate(String phoneNumber) {
        if (memberRepository.existsByPhoneNumberAndIsDeletedFalse(phoneNumber)) {
            throw new CustomException(PHONE_NUMBER_DUPLICATED);
        }
    }

    @Transactional
    public PhoneVerificationResultResponse sendPhoneVerificationCode(String phone) {
        checkPhoneNumberDuplicate(phone);

        String code = GeneratorRandomUtil.generateRandomNum();
        LocalDateTime now = LocalDateTime.now();

        redisAuthService.saveVerificationCode(phone, code);
        redisAuthService.saveVerificationTime(phone, now.toString());

        String emailAddress = emailService.getServerEmail();
        return new PhoneVerificationResultResponse(code, emailAddress);
    }

    @Transactional
    public void verifyPhoneCode(String phone) {
        String code = redisAuthService.getVerificationCode(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        String time = redisAuthService.getVerificationTime(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        LocalDateTime createdAt = LocalDateTime.parse(time);
        boolean isValid = emailService.extractCodeByPhoneNumber(code, phone, createdAt);

        if (!isValid) {
            throw new CustomException(INVALID_VERIFICATION_CODE);
        }

        redisAuthService.deleteVerificationData(phone);
        redisAuthService.markPhoneAsVerified(phone);
    }

    @Transactional
    public void withdrawMember(String uuid, WithdrawalRequest request) {
        Member member = memberRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(WRONG_ID_PW);
        }

        unlinkAllSocialAccounts(member);
        redisAuthService.deleteRefreshToken(uuid);
        redisAuthService.deleteMemberId(uuid);

        member.updateDeleteStatus();
        memberRepository.save(member);

        log.info("[Withdrawal] 회원 탈퇴 완료 - memberId: {}", member.getId());
    }

    private void validatePhoneVerification(UnifiedRegisterRequest request) {
        boolean isVerified = request.hasSocialInfo()
                ? redisAuthService.isSocialPhoneVerified(request.phoneNumber())
                : redisAuthService.isPhoneVerified(request.phoneNumber());

        if (!isVerified) {
            throw new CustomException(PHONE_NOT_VERIFIED);
        }
    }

    private SocialInfo extractSocialInfo(UnifiedRegisterRequest request) {
        if (!request.hasSocialInfo()) {
            return new SocialInfo(null, null, null, request.profileImageUrl());
        }

        String tempUuid = request.tempUuid();

        String provider = redisAuthService.getTempSocialProvider(tempUuid)
                .orElseThrow(() -> new CustomException(REAUTH_TOKEN_NOT_FOUND));

        String socialId = redisAuthService.getTempSocialId(tempUuid)
                .orElseThrow(() -> new CustomException(REAUTH_TOKEN_NOT_FOUND));

        String refreshToken = redisAuthService.getTempSocialRefreshToken(tempUuid).orElse(null);

        return new SocialInfo(provider, socialId, refreshToken, request.profileImageUrl());
    }

    private Member buildMember(UnifiedRegisterRequest request, SocialInfo socialInfo) {
        Member member = Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .nickName(request.nickName())
                .phoneNumber(request.phoneNumber())
                .profile(socialInfo.profileImageUrl())
                .socialIdKakao("kakao".equals(socialInfo.provider()) ? socialInfo.socialId() : null)
                .socialIdNaver("naver".equals(socialInfo.provider()) ? socialInfo.socialId() : null)
                .socialIdGoogle("google".equals(socialInfo.provider()) ? socialInfo.socialId() : null)
                .build();

        if (socialInfo.provider() != null && socialInfo.refreshToken() != null) {
            socialAccountLinkStrategy.linkSocialId(
                    member,
                    socialInfo.provider(),
                    socialInfo.socialId(),
                    socialInfo.refreshToken()
            );
        }

        return member;
    }

    private void cleanupRedisAfterRegister(UnifiedRegisterRequest request, SocialInfo socialInfo) {
        if (request.hasSocialInfo()) {
            String tempUuid = request.tempUuid();
            redisAuthService.clearSocialPhoneData(request.phoneNumber());
            redisAuthService.deleteTempSocialInfo(tempUuid);
        } else {
            redisAuthService.deletePhoneVerification(request.phoneNumber());
        }
    }

    private void unlinkAllSocialAccounts(Member member) {
        try {
            unlinkSocialAccountIfExists(member, "kakao", member.getSocialIdKakao(),
                    () -> member.deleteKakaoSocialId());

            unlinkSocialAccountIfExists(member, "naver", member.getSocialIdNaver(),
                    () -> {
                        member.deleteNaverSocialId();
                        member.deleteNaverRefreshToken();
                    });

            unlinkSocialAccountIfExists(member, "google", member.getSocialIdGoogle(),
                    () -> {
                        member.deleteGoogleSocialId();
                        member.deleteGoogleRefreshToken();
                    });
        } catch (Exception e) {
            log.error("[Withdrawal] 소셜 연동 해제 중 오류 발생", e);
        }
    }

    private void unlinkSocialAccountIfExists(
            Member member,
            String provider,
            String socialId,
            Runnable cleanup
    ) {
        if (socialId != null) {
            String serviceBeanName = provider + "UnlinkService";
            OAuth2UnlinkServiceProvider unlinkService = unlinkServices.get(serviceBeanName);

            if (unlinkService != null) {
                unlinkService.unlink(member);
                cleanup.run();
            }
        }
    }

    private record SocialInfo(
            String provider,
            String socialId,
            String refreshToken,
            String profileImageUrl
    ) {}
}