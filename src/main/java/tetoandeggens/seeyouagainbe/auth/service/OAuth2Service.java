package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.response.LoginResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.PhoneVerificationResultResponse;
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialLoginResultResponse;
import tetoandeggens.seeyouagainbe.auth.handler.OAuth2LoginHandler;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.provider.OAuth2TokenProvider;
import tetoandeggens.seeyouagainbe.auth.provider.OAuth2UserInfoProvider;
import tetoandeggens.seeyouagainbe.auth.util.GeneratorRandomUtil;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuth2Service {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final OAuth2TokenProvider oAuth2TokenProvider;
    private final OAuth2UserInfoProvider userInfoProvider;
    private final OAuth2LoginHandler loginHandler;
    private final EmailService emailService;

    @Transactional
    public void socialLogin(
            String provider,
            String code,
            HttpServletResponse response
    ) throws IOException {
        log.info("[OAuth2Service] 소셜 로그인 시작 - provider: {}", provider);

        try {
            String accessToken = oAuth2TokenProvider.getAccessToken(provider, code);

            String socialId = userInfoProvider.getSocialId(provider, accessToken);
            log.info("[OAuth2Service] 소셜 ID 조회 완료 - socialId: {}", socialId);

            loginHandler.handleSocialLoginCallback(provider, socialId, response);

            log.info("[OAuth2Service] 소셜 로그인 성공");
        } catch (Exception e) {
            log.error("[OAuth2Service] 소셜 로그인 실패", e);
            throw e;
        }
    }

    @Transactional
    public PhoneVerificationResultResponse sendSocialPhoneVerificationCode(
            String phone,
            String provider,
            String socialId,
            String profileImageUrl) {

        log.info("[OAuth2Service] 소셜 전화번호 인증 코드 전송 - provider: {}, phone: {}", provider, phone);

        String code = GeneratorRandomUtil.generateRandomNum();
        LocalDateTime now = LocalDateTime.now();

        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFICATION_CODE + phone, code, Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFICATION_TIME + phone, now.toString(), Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_PROVIDER + phone, provider, Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_ID + phone, socialId, Duration.ofMinutes(VERIFICATION_TIME));

        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            redisTemplate.opsForValue().set(
                    PREFIX_SOCIAL_PROFILE + phone, profileImageUrl, Duration.ofMinutes(VERIFICATION_TIME));
        }

        log.info("[OAuth2Service] 소셜 전화번호 인증 코드 저장 완료");

        String emailAddress = emailService.getServerEmail();
        return new PhoneVerificationResultResponse(code, emailAddress);
    }

    @Transactional
    public SocialLoginResultResponse verifySocialPhoneCode(String phone, HttpServletResponse response) {
        log.info("[OAuth2Service] 소셜 전화번호 인증 검증 시작 - phone: {}", phone);

        String code = redisTemplate.opsForValue().get(PREFIX_SOCIAL_VERIFICATION_CODE + phone);
        String time = redisTemplate.opsForValue().get(PREFIX_SOCIAL_VERIFICATION_TIME + phone);
        String provider = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROVIDER + phone);
        String socialId = redisTemplate.opsForValue().get(PREFIX_SOCIAL_ID + phone);

        if (code == null || time == null || provider == null || socialId == null) {
            log.warn("[OAuth2Service] 소셜 인증 정보 없음 - phone: {}", phone);
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        LocalDateTime createdAt = LocalDateTime.parse(time);
        boolean result = emailService.extractCodeByPhoneNumber(code, phone, createdAt);  // ← 수정

        if (!result) {
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFIED + phone, VERIFIED, Duration.ofMinutes(VERIFICATION_TIME));

        redisTemplate.delete(PREFIX_SOCIAL_VERIFICATION_CODE + phone);
        redisTemplate.delete(PREFIX_SOCIAL_VERIFICATION_TIME + phone);

        Optional<Member> existingMember = memberRepository.findByPhoneNumberAndIsDeletedFalse(phone);

        if (existingMember.isPresent()) {
            Member member = existingMember.get();

            boolean alreadyLinked = isAlreadyLinked(member, provider, socialId);

            if (alreadyLinked) {
                log.info("[OAuth2Service] 이미 연동된 계정 - 즉시 로그인 - memberId: {}", member.getId());
                return createLoginResponse(member, response);
            } else {
                log.info("[OAuth2Service] 계정 연동 필요 - memberId: {}, provider: {}", member.getId(), provider);
                return SocialLoginResultResponse.builder()
                        .status("LINK")
                        .message("이미 가입된 계정입니다. 소셜 계정을 연동하시겠습니까?")
                        .loginResponse(null)
                        .build();
            }
        } else {
            log.info("[OAuth2Service] 신규 회원가입 필요 - phone: {}", phone);
            return SocialLoginResultResponse.builder()
                    .status("SIGNUP")
                    .message("신규 회원가입이 필요합니다. /auth/signup으로 요청하세요.")
                    .loginResponse(null)
                    .build();
        }
    }

    @Transactional
    public SocialLoginResultResponse linkSocialAccount(String phone, HttpServletResponse response) {
        log.info("[OAuth2Service] 소셜 계정 연동 시작 - phone: {}", phone);

        String verified = redisTemplate.opsForValue().get(PREFIX_SOCIAL_VERIFIED + phone);
        String socialId = redisTemplate.opsForValue().get(PREFIX_SOCIAL_ID + phone);
        String provider = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROVIDER + phone);
        String profileImageUrl = redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROFILE + phone);

        if (verified == null || !verified.equals(VERIFIED)) {
            log.warn("[OAuth2Service] 인증되지 않은 전화번호 - phone: {}", phone);
            throw new CustomException(AuthErrorCode.PHONE_NOT_VERIFIED);
        }

        Member member = memberRepository.findByPhoneNumberAndIsDeletedFalse(phone)
                .orElseThrow(() -> {
                    log.error("[OAuth2Service] 회원을 찾을 수 없음 - phone: {}", phone);
                    return new CustomException(AuthErrorCode.MEMBER_NOT_FOUND);
                });

        linkSocialIdToMember(member, provider, socialId);

        if (profileImageUrl != null && !profileImageUrl.isBlank() && member.getProfile() == null) {
            member.updateProfile(profileImageUrl);
        }

        log.info("[OAuth2Service] 소셜 계정 연동 완료 - memberId: {}, provider: {}", member.getId(), provider);

        clearSocialRedisData(phone);

        return createLoginResponse(member, response);
    }

    private boolean isAlreadyLinked(Member member, String provider, String socialId) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> member.getSocialIdKakao() != null && member.getSocialIdKakao().equals(socialId);
            case "naver" -> member.getSocialIdNaver() != null && member.getSocialIdNaver().equals(socialId);
            case "google" -> member.getSocialIdGoogle() != null && member.getSocialIdGoogle().equals(socialId);
            default -> false;
        };
    }

    private void linkSocialIdToMember(Member member, String provider, String socialId) {
        switch (provider.toLowerCase()) {
            case "kakao" -> member.updateKakaoSocialId(socialId);
            case "naver" -> member.updateNaverSocialId(socialId);
            case "google" -> member.updateGoogleSocialId(socialId);
        }
    }

    private SocialLoginResultResponse createLoginResponse(Member member, HttpServletResponse response) {
        log.info("[OAuth2Service] 로그인 토큰 생성 - memberId: {}", member.getId());

        UserTokenResponse tokenResponse = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );
        tokenProvider.setRefreshTokenCookie(response, tokenResponse.refreshToken());

        LoginResponse loginResponse = LoginResponse.builder()
                .uuid(member.getUuid())
                .role(member.getRole().getRole())
                .accessToken(tokenResponse.accessToken())
                .build();

        return SocialLoginResultResponse.builder()
                .status("LOGIN")
                .message("로그인 성공")
                .loginResponse(loginResponse)
                .build();
    }

    private void clearSocialRedisData(String phone) {
        redisTemplate.delete(PREFIX_SOCIAL_VERIFIED + phone);
        redisTemplate.delete(PREFIX_SOCIAL_PROVIDER + phone);
        redisTemplate.delete(PREFIX_SOCIAL_ID + phone);
        redisTemplate.delete(PREFIX_SOCIAL_PROFILE + phone);
        log.info("[OAuth2Service] Redis 소셜 데이터 정리 - phone: {}", phone);
    }
}
