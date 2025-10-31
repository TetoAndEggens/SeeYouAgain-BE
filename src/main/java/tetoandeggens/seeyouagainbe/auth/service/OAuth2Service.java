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
import tetoandeggens.seeyouagainbe.auth.dto.response.SocialTempInfoResponse;
import tetoandeggens.seeyouagainbe.auth.jwt.TokenProvider;
import tetoandeggens.seeyouagainbe.auth.jwt.UserTokenResponse;
import tetoandeggens.seeyouagainbe.auth.util.GeneratorRandomUtil;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

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
    private final EmailService emailService;

    @Transactional
    public PhoneVerificationResultResponse sendSocialPhoneVerificationCode(
            String phone,
            String provider,
            String socialId,
            String profileImageUrl
    ) {
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

        String emailAddress = emailService.getServerEmail();
        return new PhoneVerificationResultResponse(code, emailAddress);
    }

    @Transactional
    public SocialLoginResultResponse verifySocialPhoneCode(String phone, HttpServletResponse response) {

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

        String tempKey = "oauth2:refresh:temp:" + provider + ":" + socialId;
        String refreshToken = redisTemplate.opsForValue().get(tempKey);

        linkSocialIdToMember(member, provider, socialId, refreshToken);

        if (profileImageUrl != null && !profileImageUrl.isBlank() && member.getProfile() == null) {
            member.updateProfile(profileImageUrl);
        }

        log.info("[OAuth2Service] 소셜 계정 연동 완료 - memberId: {}, provider: {}", member.getId(), provider);

        clearSocialRedisData(phone);

        if (refreshToken != null) {
            redisTemplate.delete(tempKey);
        }

        return createLoginResponse(member, response);
    }

    @Transactional(readOnly = true)
    public SocialTempInfoResponse getSocialTempInfo(String token) {
        String data = redisTemplate.opsForValue().get("signup:temp:" + token);
        if (data == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        String[] parts = data.split(":::");
        if (parts.length < 2) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        String provider = parts[0];
        String socialId = parts[1];
        String profileImageUrl = parts.length > 2 ? parts[2] : null;

        return new SocialTempInfoResponse(provider, socialId, profileImageUrl);
    }

    private boolean isAlreadyLinked(Member member, String provider, String socialId) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> member.getSocialIdKakao() != null && member.getSocialIdKakao().equals(socialId);
            case "naver" -> member.getSocialIdNaver() != null && member.getSocialIdNaver().equals(socialId);
            case "google" -> member.getSocialIdGoogle() != null && member.getSocialIdGoogle().equals(socialId);
            default -> false;
        };
    }

    private void linkSocialIdToMember(Member member, String provider, String socialId, String refreshToken) {
        switch (provider.toLowerCase()) {
            case "kakao" -> member.updateKakaoSocialId(socialId);
            case "naver" -> {
                member.updateNaverSocialId(socialId);
                if (refreshToken != null) {
                    member.updateNaverRefreshToken(refreshToken);
                    log.info("[OAuth2Service] 네이버 RefreshToken 저장 완료");
                }
            }
            case "google" -> {
                member.updateGoogleSocialId(socialId);
                if (refreshToken != null) {
                    member.updateGoogleRefreshToken(refreshToken);
                    log.info("[OAuth2Service] 구글 RefreshToken 저장 완료");
                }
            }
        }
        memberRepository.save(member);
    }

    private SocialLoginResultResponse createLoginResponse(Member member, HttpServletResponse response) {
        log.info("[OAuth2Service] 로그인 토큰 생성 - memberId: {}", member.getId());

        UserTokenResponse tokenResponse = tokenProvider.createLoginToken(
                member.getUuid(),
                member.getRole()
        );
        tokenProvider.setAccessTokenCookie(response, tokenResponse.accessToken());
        tokenProvider.setRefreshTokenCookie(response, tokenResponse.refreshToken());

        LoginResponse loginResponse = LoginResponse.builder()
                .uuid(member.getUuid())
                .role(member.getRole().getRole())
//                .accessToken(tokenResponse.accessToken())
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
        redisTemplate.delete(PREFIX_SOCIAL_REFRESH_TOKEN + phone);
        log.info("[OAuth2Service] Redis 소셜 데이터 정리 - phone: {}", phone);
    }
}