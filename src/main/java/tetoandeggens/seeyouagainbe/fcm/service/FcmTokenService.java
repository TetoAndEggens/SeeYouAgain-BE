package tetoandeggens.seeyouagainbe.fcm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.fcm.dto.request.FcmTokenRequest;
import tetoandeggens.seeyouagainbe.fcm.dto.response.FcmTokenResponse;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;
import tetoandeggens.seeyouagainbe.fcm.repository.FcmTokenRepository;
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

    @Transactional
    public FcmTokenResponse saveOrUpdateToken(Long memberId, FcmTokenRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.MEMBER_NOT_FOUND));

        if (!firebaseMessagingService.isValidToken(request.token())) {
            log.warn("유효하지 않은 FCM 토큰 - MemberId: {}, Token: {}...",
                    memberId, request.token().substring(0, 20));
            throw new CustomException(FcmErrorCode.INVALID_TOKEN);
        }

        FcmToken fcmToken = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, request.deviceId())
                .map(existingToken -> {
                    existingToken.updateToken(request.token());
                    log.info("FCM 토큰 업데이트 완료 - MemberId: {}, DeviceId: {}", memberId, request.deviceId());
                    return existingToken;
                })
                .orElseGet(() -> {
                    FcmToken newToken = FcmToken.builder()
                            .token(request.token())
                            .deviceId(request.deviceId())
                            .deviceType(request.deviceType())
                            .lastUsedAt(LocalDateTime.now())
                            .member(member)
                            .build();
                    log.info("새 FCM 토큰 저장 완료 - MemberId: {}, DeviceId: {}", memberId, request.deviceId());
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
                        log.info("FCM 토큰 갱신 완료 - MemberId: {}, DeviceId: {}", memberId, deviceId);
                    }
                });
    }

    @Transactional
    public void deleteToken(Long memberId, String deviceId) {
        FcmToken fcmToken = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, deviceId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.TOKEN_NOT_FOUND));


        fcmTokenRepository.delete(fcmToken);
        log.info("FCM 토큰 삭제 완료 - MemberId: {}, DeviceId: {}", memberId, deviceId);
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

        log.info("만료된 FCM 토큰 정리 시작 - 개수: {}", expiredTokens.size());

        expiredTokens.forEach(token -> {
            try {
                fcmTokenRepository.delete(token);
                log.info("만료된 FCM 토큰 삭제 완료 - TokenId: {}", token.getId());
            } catch (Exception e) {
                log.error("만료된 FCM 토큰 삭제 실패 - TokenId: {}", token.getId(), e);
            }
        });

        log.info("만료된 FCM 토큰 정리 완료 - 처리된 개수: {}", expiredTokens.size());
    }
}