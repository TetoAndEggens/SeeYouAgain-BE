package tetoandeggens.seeyouagainbe.member.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;

@Entity
@Table(name = "MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(name = "login_id", unique = true)
	private String loginId;

	@Column(name = "password")
	private String password;

	@Column(name = "nick_name", nullable = false)
	private String nickName;

	@Column(name = "phone_number", nullable = false, unique = true)
	private String phoneNumber;

	@Column(name = "profile")
	private String profile;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private Role role = Role.USER;

	@Column(name = "uuid", unique = true, nullable = false)
	private String uuid;

	@Column(name = "social_id_kakao", unique = true)
	private String socialIdKakao;

	@Column(name = "social_id_naver", unique = true)
	private String socialIdNaver;

	@Column(name = "social_id_google", unique = true)
	private String socialIdGoogle;

	@Column(name = "google_refresh_token")
	private String googleRefreshToken;

	@Column(name = "naver_refresh_token")
	private String naverRefreshToken;

	@Column(name = "violated_count", nullable = false)
	private Long violatedCount = 0L;

	@Column(name = "is_push_enabled", nullable = false)
	private Boolean isPushEnabled = false;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@Column(name = "is_banned")
	private Boolean isBanned = false;

	@Builder
	public Member(String loginId, String password, String nickName, String phoneNumber,
		String profile, String socialIdKakao, String socialIdNaver, String socialIdGoogle) {
		this.loginId = loginId;
		this.password = password;
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
		this.profile = profile;
		this.socialIdKakao = socialIdKakao;
		this.socialIdNaver = socialIdNaver;
		this.socialIdGoogle = socialIdGoogle;
		this.uuid = UUID.randomUUID().toString();
	}

	public Member(Long memberId) {
		this.id = memberId;
	}

    // 관리자 승인 시 사용 예정
    public void increaseViolatedCount() {
        this.violatedCount++;

        if (this.violatedCount >= 3) {
            this.isBanned = true;
        }
    }

	public void updateProfile(String profileImageUrl) {
		this.profile = profileImageUrl;
	}

	public void updateDeleteStatus() {
		this.isDeleted = true;
	}

	public void updatePushEnabled(Boolean isPushEnabled) {
		this.isPushEnabled = isPushEnabled;
	}

	public void updateKakaoSocialId(String socialIdKakao) {
		this.socialIdKakao = socialIdKakao;
	}

	public void updateNaverSocialId(String socialIdNaver) {
		this.socialIdNaver = socialIdNaver;
	}

	public void updateGoogleSocialId(String socialIdGoogle) {
		this.socialIdGoogle = socialIdGoogle;
	}

    public void updateGoogleRefreshToken(String googleRefreshToken) {
        this.googleRefreshToken = googleRefreshToken;
    }

    public void updateNaverRefreshToken(String naverRefreshToken) {
        this.naverRefreshToken = naverRefreshToken;
    }

	public void deleteKakaoSocialId() {
		this.socialIdKakao = null;
	}

	public void deleteNaverSocialId() {
		this.socialIdNaver = null;
	}

	public void deleteGoogleSocialId() {
		this.socialIdGoogle = null;
	}

	public void deleteGoogleRefreshToken() {
		this.googleRefreshToken = null;
	}

	public void deleteNaverRefreshToken() {
		this.naverRefreshToken = null;
	}
}