package tetoandeggens.seeyouagainbe.fcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;
import tetoandeggens.seeyouagainbe.fcm.repository.custom.FcmTokenRepositoryCustom;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long>, FcmTokenRepositoryCustom {

    Optional<FcmToken> findByMemberIdAndDeviceId(Long memberId, String deviceId);

    List<FcmToken> findAllByMemberId(Long memberId);
}