package tetoandeggens.seeyouagainbe.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SocialType type;

    @Column(name = "social_id")
    private String socialId;

    @Column(name = "violated_count", nullable = false)
    private Long violatedCount = 0L;

    @Column(name = "is_push_enabled", nullable = false)
    private Boolean isPushEnabled = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder
    public Member(String loginId, String password, String nickName, String phoneNumber,
                  String socialId, SocialType type) {
        this.loginId = loginId;
        this.password = password;
        this.nickName = nickName;
        this.phoneNumber = phoneNumber;
        this.socialId = socialId;
        this.type = type;
        this.uuid = UUID.randomUUID().toString();
    }

    public void updateDeleteStatus() {
        this.isDeleted = true;
    }

    public void updatePushEnabled(Boolean isPushEnabled) {
        this.isPushEnabled = isPushEnabled;
    }

    public void updateSocialInfo(String socialId, SocialType type) {
        this.socialId = socialId;
        this.type = type;
    }

    public void deleteOAuthInfo() {
        this.socialId = null;
        this.type = null;
    }
}
