package tetoandeggens.seeyouagainbe.fcm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.fcm.dto.request.FcmTokenRequest;
import tetoandeggens.seeyouagainbe.fcm.dto.response.FcmTokenResponse;
import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;
import tetoandeggens.seeyouagainbe.fcm.repository.FcmTokenRepository;
import tetoandeggens.seeyouagainbe.fcm.util.DeviceTypeValidator;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final MemberRepository memberRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final DeviceTypeValidator deviceTypeValidator;

    @Transactional
    public FcmTokenResponse saveOrUpdateToken(
            Long memberId,
            FcmTokenRequest request,
            String userAgent
    ) {
        // 1. Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.MEMBER_NOT_FOUND));

        // 2. Firebase 토큰 유효성 검증
        if (!firebaseMessagingService.isValidToken(request.token())) {
            log.warn("유효하지 않은 FCM 토큰 - MemberId: {}, Token: {}...",
                    memberId, request.token().substring(0, 20));
            throw new CustomException(FcmErrorCode.INVALID_TOKEN);
        }

        // 3. User-Agent에서 deviceType 자동 추출
        DeviceType deviceType = deviceTypeValidator.validateAndExtractDeviceType(userAgent);

        // 4. 기존 토큰 존재 여부 확인 및 저장/업데이트
        FcmToken fcmToken = fcmTokenRepository
                .findByMemberIdAndDeviceId(memberId, request.deviceId())
                .map(existingToken -> {
                    existingToken.updateToken(request.token());
                    return existingToken;
                })
                .orElseGet(() -> {
                    FcmToken newToken = FcmToken.builder()
                            .token(request.token())
                            .deviceId(request.deviceId())
                            .deviceType(deviceType)
                            .lastUsedAt(LocalDateTime.now())
                            .member(member)
                            .build();
                    return fcmTokenRepository.save(newToken);
                });

        return FcmTokenResponse.from(fcmToken);
    }

    @Transactional
    public void refreshTokenIfNeeded(Long memberId, String deviceId) {
        fcmTokenRepository.findByMemberIdAndDeviceId(memberId, deviceId)
                .ifPresent(token -> {
                    if (token.needsRefresh()) {
                        token.updateLastUsedAt();
                    }
                });
    }

    @Transactional
    public void deleteToken(Long memberId, String deviceId) {
        FcmToken fcmToken = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, deviceId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.TOKEN_NOT_FOUND));

        fcmTokenRepository.delete(fcmToken);
    }

    public List<FcmTokenResponse> getTokensByMemberId(Long memberId) {
        return fcmTokenRepository.findAllByMemberId(memberId).stream()
                .map(FcmTokenResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cleanupExpiredTokens(int expirationDays) {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(expirationDays);
        List<FcmToken> expiredTokens = fcmTokenRepository.findExpiredTokens(expirationDate);

        expiredTokens.forEach(token -> {
            try {
                fcmTokenRepository.delete(token);
                log.info("만료된 FCM 토큰 삭제 완료 - TokenId: {}", token.getId());
            } catch (Exception e) {
                log.error("만료된 FCM 토큰 삭제 실패 - TokenId: {}", token.getId(), e);
            }
        });
    }
}