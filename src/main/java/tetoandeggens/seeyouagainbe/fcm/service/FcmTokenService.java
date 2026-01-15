package tetoandeggens.seeyouagainbe.fcm.service;

import lombok.RequiredArgsConstructor;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final DeviceTypeValidator deviceTypeValidator;

    @Transactional
    public FcmTokenResponse saveOrUpdateToken(
            Long memberId,
            FcmTokenRequest request,
            String userAgent
    ) {
        if (!firebaseMessagingService.isValidToken(request.token())) {
            throw new CustomException(FcmErrorCode.INVALID_TOKEN);
        }

        DeviceType deviceType = deviceTypeValidator.validateAndExtractDeviceType(userAgent);

        FcmToken fcmToken = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, request.deviceId())
                .orElse(null);

        if (fcmToken != null) {
            fcmToken.updateToken(request.token());
        } else {
            FcmToken newToken = FcmToken.builder()
                    .token(request.token())
                    .deviceId(request.deviceId())
                    .deviceType(deviceType)
                    .lastUsedAt(LocalDateTime.now())
                    .member(new Member(memberId))
                    .build();

            fcmToken = fcmTokenRepository.save(newToken);
        }

        return FcmTokenResponse.from(fcmToken);
    }

    @Transactional
    public void refreshTokenIfNeeded(Long memberId, String deviceId) {
        FcmToken fcmToken = fcmTokenRepository
                .findByMemberIdAndDeviceId(memberId, deviceId)
                .orElse(null);

        if (fcmToken == null) {
            return;
        }

        if (fcmToken.needsRefresh()) {
            fcmToken.updateLastUsedAt();
        }
    }

    @Transactional
    public void deleteToken(Long memberId, String deviceId) {
        FcmToken fcmToken = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, deviceId)
                .orElseThrow(() -> new CustomException(FcmErrorCode.TOKEN_NOT_FOUND));

        fcmTokenRepository.delete(fcmToken);
    }

    @Transactional(readOnly = true)
    public List<FcmTokenResponse> getTokensByMemberId(Long memberId) {
        List<FcmToken> fcmTokens = fcmTokenRepository.findAllByMemberId(memberId);
        List<FcmTokenResponse> responses = new ArrayList<>();

        for (FcmToken token : fcmTokens) {
            FcmTokenResponse response = FcmTokenResponse.from(token);
            responses.add(response);
        }

        return responses;
    }
}