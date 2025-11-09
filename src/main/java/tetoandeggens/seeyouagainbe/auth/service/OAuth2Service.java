package tetoandeggens.seeyouagainbe.auth.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;
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
    public PhoneVerificationResultResponse sendSocialPhoneVerificationCode(String phone, String tempUuid) {
        // 1. tempUuid로 Redis에서 소셜 정보 조회
        String provider = redisTemplate.opsForValue().get(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid);
        String socialId = redisTemplate.opsForValue().get(PREFIX_TEMP_SOCIAL_ID + tempUuid);

        if (provider == null || socialId == null) {
            log.error("[OAuth2Service] tempUuid로 소셜 정보를 찾을 수 없음 - tempUuid: {}", tempUuid);
            throw new CustomException(AuthErrorCode.REAUTH_TOKEN_NOT_FOUND);
        }

        log.info("[OAuth2Service] 소셜 정보 조회 성공 - tempUuid: {}, provider: {}", tempUuid, provider);

        // 2. 인증 코드 생성
        String code = GeneratorRandomUtil.generateRandomNum();
        LocalDateTime now = LocalDateTime.now();

        // 3. Redis에 인증 코드 및 소셜 정보 저장
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFICATION_CODE + phone, code, Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFICATION_TIME + phone, now.toString(), Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_PROVIDER + phone, provider, Duration.ofMinutes(VERIFICATION_TIME));
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_ID + phone, socialId, Duration.ofMinutes(VERIFICATION_TIME));

        // 4. tempUuid 저장 (나중에 회원가입 시 사용)
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_TEMP_UUID + phone, tempUuid, Duration.ofMinutes(VERIFICATION_TIME));

        log.info("[OAuth2Service] 소셜 휴대폰 인증 코드 생성 완료 - phone: {}, provider: {}", phone, provider);

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
        boolean result = emailService.extractCodeByPhoneNumber(code, phone, createdAt);

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

        if (verified == null || !verified.equals(VERIFIED)) {
            log.warn("[OAuth2Service] 인증되지 않은 전화번호 - phone: {}", phone);
            throw new CustomException(AuthErrorCode.PHONE_NOT_VERIFIED);
        }

        Member member = memberRepository.findByPhoneNumberAndIsDeletedFalse(phone)
                .orElseThrow(() -> {
                    log.error("[OAuth2Service] 회원을 찾을 수 없음 - phone: {}", phone);
                    return new CustomException(AuthErrorCode.MEMBER_NOT_FOUND);
                });

        String tempUuid = redisTemplate.opsForValue().get(PREFIX_SOCIAL_TEMP_UUID + phone);

        // tempUuid로 RefreshToken 조회
        String refreshToken = null;
        if (tempUuid != null) {
            refreshToken = redisTemplate.opsForValue().get(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid);
            log.info("[OAuth2Service] RefreshToken 조회 - tempUuid: {}, found: {}",
                    tempUuid, refreshToken != null);
        }

        linkSocialIdToMember(member, provider, socialId, refreshToken);

        log.info("[OAuth2Service] 소셜 계정 연동 완료 - memberId: {}, provider: {}", member.getId(), provider);

        clearSocialRedisData(phone);

        if (tempUuid != null) {
            redisTemplate.delete(PREFIX_TEMP_SOCIAL_ID + tempUuid);
            redisTemplate.delete(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid);
            redisTemplate.delete(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid);
            log.info("[OAuth2Service] Redis 임시 UUID 데이터 삭제 - tempUuid: {}", tempUuid);
        }

        return createLoginResponse(member, response);
    }

    @Transactional(readOnly = true)
    public SocialTempInfoResponse getSocialTempInfo(HttpServletRequest request) {
        String token = CookieUtil.resolveCookieValue(request, "socialTempToken");
        if (token == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        Claims claims = tokenProvider.parseClaims(token);

        String profileImageUrl = claims.get("profileImageUrl", String.class);
        String tempUuid = claims.get("tempUuid", String.class);

        if (tempUuid == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        extendRedisTTL(tempUuid);
        return new SocialTempInfoResponse(profileImageUrl, tempUuid);
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
                .build();

        return SocialLoginResultResponse.builder()
                .status("LOGIN")
                .message("로그인 성공")
                .loginResponse(loginResponse)
                .build();
    }

    private void extendRedisTTL(String tempUuid) {
        Duration ttl = Duration.ofMinutes(VERIFICATION_TIME);

        // 각 키의 TTL을 10분씩 연장
        redisTemplate.expire(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid, ttl);
        redisTemplate.expire(PREFIX_TEMP_SOCIAL_ID + tempUuid, ttl);
        redisTemplate.expire(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid, ttl);

        log.info("[OAuth2Service] Redis TTL 연장 완료 - tempUuid: {}, 추가 TTL: {}분", tempUuid, VERIFICATION_TIME);
    }

    private void clearSocialRedisData(String phone) {
        redisTemplate.delete(PREFIX_SOCIAL_VERIFIED + phone);
        redisTemplate.delete(PREFIX_SOCIAL_PROVIDER + phone);
        redisTemplate.delete(PREFIX_SOCIAL_ID + phone);
        redisTemplate.delete(PREFIX_SOCIAL_TEMP_UUID + phone);
        log.info("[OAuth2Service] Redis 소셜 데이터 정리 - phone: {}", phone);
    }
}