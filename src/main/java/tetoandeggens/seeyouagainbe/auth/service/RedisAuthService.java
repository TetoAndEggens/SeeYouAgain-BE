package tetoandeggens.seeyouagainbe.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

import static tetoandeggens.seeyouagainbe.global.constants.AuthVerificationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAuthService { // Redis를 사용한 인증 관련 데이터 관리 서비스

    private final RedisTemplate<String, String> redisTemplate;

    // ============ 일반 휴대폰 인증 ============

    public void saveVerificationCode(String phone, String code) {
        redisTemplate.opsForValue().set(
                PREFIX_VERIFICATION_CODE + phone,
                code,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public void saveVerificationTime(String phone, String time) {
        redisTemplate.opsForValue().set(
                PREFIX_VERIFICATION_TIME + phone,
                time,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public Optional<String> getVerificationCode(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_VERIFICATION_CODE + phone)
        );
    }

    public Optional<String> getVerificationTime(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_VERIFICATION_TIME + phone)
        );
    }

    public void markPhoneAsVerified(String phone) {
        redisTemplate.opsForValue().set(
                phone,
                VERIFIED,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public boolean isPhoneVerified(String phone) {
        String verified = redisTemplate.opsForValue().get(phone);
        return VERIFIED.equals(verified);
    }

    public void deleteVerificationData(String phone) {
        redisTemplate.delete(PREFIX_VERIFICATION_CODE + phone);
        redisTemplate.delete(PREFIX_VERIFICATION_TIME + phone);
    }

    public void deletePhoneVerification(String phone) {
        redisTemplate.delete(phone);
    }

    // ============ 소셜 로그인 휴대폰 인증 ============

    public void saveSocialVerificationCode(String phone, String code) {
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFICATION_CODE + phone,
                code,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public void saveSocialVerificationTime(String phone, String time) {
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFICATION_TIME + phone,
                time,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public void saveSocialProvider(String phone, String provider) {
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_PROVIDER + phone,
                provider,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public void saveSocialId(String phone, String socialId) {
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_ID + phone,
                socialId,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public void saveSocialTempUuid(String phone, String tempUuid) {
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_TEMP_UUID + phone,
                tempUuid,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public Optional<String> getSocialVerificationCode(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_SOCIAL_VERIFICATION_CODE + phone)
        );
    }

    public Optional<String> getSocialVerificationTime(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_SOCIAL_VERIFICATION_TIME + phone)
        );
    }

    public Optional<String> getSocialProvider(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_SOCIAL_PROVIDER + phone)
        );
    }

    public Optional<String> getSocialId(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_SOCIAL_ID + phone)
        );
    }

    public Optional<String> getSocialTempUuid(String phone) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_SOCIAL_TEMP_UUID + phone)
        );
    }

    public void markSocialPhoneAsVerified(String phone) {
        redisTemplate.opsForValue().set(
                PREFIX_SOCIAL_VERIFIED + phone,
                VERIFIED,
                Duration.ofMinutes(VERIFICATION_TIME)
        );
    }

    public boolean isSocialPhoneVerified(String phone) {
        String verified = redisTemplate.opsForValue().get(PREFIX_SOCIAL_VERIFIED + phone);
        return VERIFIED.equals(verified);
    }

    public void deleteSocialVerificationData(String phone) {
        redisTemplate.delete(PREFIX_SOCIAL_VERIFICATION_CODE + phone);
        redisTemplate.delete(PREFIX_SOCIAL_VERIFICATION_TIME + phone);
    }

    public void clearSocialPhoneData(String phone) {
        redisTemplate.delete(PREFIX_SOCIAL_VERIFIED + phone);
        redisTemplate.delete(PREFIX_SOCIAL_PROVIDER + phone);
        redisTemplate.delete(PREFIX_SOCIAL_ID + phone);
        redisTemplate.delete(PREFIX_SOCIAL_TEMP_UUID + phone);
    }

    // ============ 소셜 임시 정보 (tempUuid 기반) ============

    public void saveTempSocialInfo(String tempUuid, String provider, String socialId, String refreshToken) {
        Duration ttl = Duration.ofMinutes(VERIFICATION_TIME);

        redisTemplate.opsForValue().set(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid, provider, ttl);
        redisTemplate.opsForValue().set(PREFIX_TEMP_SOCIAL_ID + tempUuid, socialId, ttl);

        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.opsForValue().set(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid, refreshToken, ttl);
        }
    }

    public Optional<String> getTempSocialProvider(String tempUuid) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid)
        );
    }

    public Optional<String> getTempSocialId(String tempUuid) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_TEMP_SOCIAL_ID + tempUuid)
        );
    }

    public Optional<String> getTempSocialRefreshToken(String tempUuid) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid)
        );
    }

    public void extendTempSocialInfoTTL(String tempUuid) {
        Duration ttl = Duration.ofMinutes(VERIFICATION_TIME);
        redisTemplate.expire(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid, ttl);
        redisTemplate.expire(PREFIX_TEMP_SOCIAL_ID + tempUuid, ttl);
        redisTemplate.expire(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid, ttl);
    }

    public void deleteTempSocialInfo(String tempUuid) {
        redisTemplate.delete(PREFIX_TEMP_SOCIAL_PROVIDER + tempUuid);
        redisTemplate.delete(PREFIX_TEMP_SOCIAL_ID + tempUuid);
        redisTemplate.delete(PREFIX_TEMP_SOCIAL_REFRESH + tempUuid);
    }

    // ============ JWT Refresh Token ============

    public void saveRefreshToken(String uuid, String refreshToken, long expirationMs) {
        redisTemplate.opsForValue().set(
                uuid,
                refreshToken,
                Duration.ofMillis(expirationMs)
        );
    }

    public Optional<String> getRefreshToken(String uuid) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(uuid));
    }

    public void deleteRefreshToken(String uuid) {
        redisTemplate.delete(uuid);
    }
}
