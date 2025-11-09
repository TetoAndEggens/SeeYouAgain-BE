package tetoandeggens.seeyouagainbe.auth.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.response.LoginResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialTempInfoResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.util.GeneratorRandomUtil;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.time.LocalDateTime;

import static tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuth2Service {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final RedisAuthService redisAuthService;
    private final CookieService cookieService;
    private final EmailService emailService;
    private final SocialAccountLinkStrategy socialAccountLinkStrategy;

    @Transactional
    public PhoneVerificationResultResponse sendSocialPhoneVerificationCode(String phone, String tempUuid) {
        String provider = redisAuthService.getTempSocialProvider(tempUuid)
                .orElseThrow(() -> new CustomException(REAUTH_TOKEN_NOT_FOUND));

        String socialId = redisAuthService.getTempSocialId(tempUuid)
                .orElseThrow(() -> new CustomException(REAUTH_TOKEN_NOT_FOUND));

        String code = GeneratorRandomUtil.generateRandomNum();
        LocalDateTime now = LocalDateTime.now();

        redisAuthService.saveSocialVerificationCode(phone, code);
        redisAuthService.saveSocialVerificationTime(phone, now.toString());
        redisAuthService.saveSocialProvider(phone, provider);
        redisAuthService.saveSocialId(phone, socialId);
        redisAuthService.saveSocialTempUuid(phone, tempUuid);

        String emailAddress = emailService.getServerEmail();
        return new PhoneVerificationResultResponse(code, emailAddress);
    }

    @Transactional
    public SocialLoginResultResponse verifySocialPhoneCode(String phone, HttpServletResponse response) {
        String code = redisAuthService.getSocialVerificationCode(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        String time = redisAuthService.getSocialVerificationTime(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        String provider = redisAuthService.getSocialProvider(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        String socialId = redisAuthService.getSocialId(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        LocalDateTime createdAt = LocalDateTime.parse(time);
        boolean isValid = emailService.extractCodeByPhoneNumber(code, phone, createdAt);

        if (!isValid) {
            throw new CustomException(INVALID_VERIFICATION_CODE);
        }

        redisAuthService.markSocialPhoneAsVerified(phone);
        redisAuthService.deleteSocialVerificationData(phone);

        return memberRepository.findByPhoneNumberAndIsDeletedFalse(phone)
                .map(member -> handleExistingMember(member, provider, socialId, response))
                .orElseGet(this::handleNewMember);
    }

    @Transactional
    public SocialLoginResultResponse linkSocialAccount(String phone, HttpServletResponse response) {
        if (!redisAuthService.isSocialPhoneVerified(phone)) {
            throw new CustomException(PHONE_NOT_VERIFIED);
        }

        String socialId = redisAuthService.getSocialId(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        String provider = redisAuthService.getSocialProvider(phone)
                .orElseThrow(() -> new CustomException(INVALID_VERIFICATION_CODE));

        Member member = memberRepository.findByPhoneNumberAndIsDeletedFalse(phone)
                .orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND));

        String tempUuid = redisAuthService.getSocialTempUuid(phone).orElse(null);
        String refreshToken = null;

        if (tempUuid != null) {
            refreshToken = redisAuthService.getTempSocialRefreshToken(tempUuid).orElse(null);
        }

        socialAccountLinkStrategy.linkSocialId(member, provider, socialId, refreshToken);
        memberRepository.save(member);

        cleanupSocialRedisData(phone, tempUuid);

        return createLoginResponse(member, response);
    }

    @Transactional(readOnly = true)
    public SocialTempInfoResponse getSocialTempInfo(HttpServletRequest request) {
        String token = cookieService.resolveSocialTempToken(request);
        if (token == null) {
            throw new CustomException(INVALID_TOKEN);
        }

        Claims claims = tokenProvider.parseClaims(token);
        String profileImageUrl = claims.get("profileImageUrl", String.class);
        String tempUuid = claims.get("tempUuid", String.class);

        if (tempUuid == null) {
            throw new CustomException(INVALID_TOKEN);
        }

        redisAuthService.extendTempSocialInfoTTL(tempUuid);

        return new SocialTempInfoResponse(profileImageUrl, tempUuid);
    }

    private SocialLoginResultResponse handleExistingMember(
            Member member,
            String provider,
            String socialId,
            HttpServletResponse response
    ) {
        boolean alreadyLinked = socialAccountLinkStrategy.isAlreadyLinked(member, provider, socialId);

        if (alreadyLinked) {
            return createLoginResponse(member, response);
        } else {
            return SocialLoginResultResponse.builder()
                    .status("LINK")
                    .message("이미 가입된 계정입니다. 소셜 계정을 연동하시겠습니까?")
                    .loginResponse(null)
                    .build();
        }
    }

    private SocialLoginResultResponse handleNewMember() {
        return SocialLoginResultResponse.builder()
                .status("SIGNUP")
                .message("신규 회원가입이 필요합니다. /auth/signup으로 요청하세요.")
                .loginResponse(null)
                .build();
    }

    private SocialLoginResultResponse createLoginResponse(Member member, HttpServletResponse response) {
        UserTokenResponse tokenResponse = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );

        cookieService.setAccessTokenCookie(
                response,
                tokenResponse.accessToken(),
                tokenProvider.getAccessTokenExpirationSec()
        );
        cookieService.setRefreshTokenCookie(
                response,
                tokenResponse.refreshToken(),
                tokenProvider.getRefreshTokenExpirationSec()
        );

        redisAuthService.saveRefreshToken(
                member.getUuid(),
                tokenResponse.refreshToken(),
                tokenProvider.getRefreshTokenExpirationMs()
        );

        LoginResponse loginResponse = LoginResponse.builder()
                .uuid(member.getUuid())
                .role(member.getRole().getRole())
                .build();

        return SocialLoginResultResponse.builder()
                .status("LOGIN")
                .message("로그인 성공")
                .loginResponse(loginResponse)
                .build();
    }

    private void cleanupSocialRedisData(String phone, String tempUuid) {
        redisAuthService.clearSocialPhoneData(phone);

        if (tempUuid != null) {
            redisAuthService.deleteTempSocialInfo(tempUuid);
        }
    }
}