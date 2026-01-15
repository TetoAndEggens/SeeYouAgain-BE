package tetoandeggens.seeyouagainbe.fcm.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Entity
@Table(name = "FCM_TOKEN")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "fcm_token_id")
	private Long id;

	@Column(name = "token")
	private String token;

	@Column(name = "device_id")
	private String deviceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "device_type")
	private DeviceType deviceType;

	@Column(name = "last_used_at")
	private LocalDateTime lastUsedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

    @Builder
    public FcmToken(String token, String deviceId, DeviceType deviceType,
                    LocalDateTime lastUsedAt, Member member) {
        this.token = token;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.lastUsedAt = lastUsedAt != null ? lastUsedAt : LocalDateTime.now();
        this.member = member;
    }

    public void updateToken(String newToken) {
        this.token = newToken;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean needsRefresh() {
        LocalDateTime refreshDate = LocalDateTime.now().minusDays(30);
        return this.lastUsedAt.isBefore(refreshDate);
    }
}