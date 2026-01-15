package tetoandeggens.seeyouagainbe.fcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.fcm.entity.FcmToken;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByMemberIdAndDeviceId(Long memberId, String deviceId);

    List<FcmToken> findAllByMemberId(Long memberId);
}