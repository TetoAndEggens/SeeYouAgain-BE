package tetoandeggens.seeyouagainbe.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import java.util.UUID;

@Entity
@Table(name = "MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "login_id", unique = true)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "profile")
    private String profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SocialType type;

    @Column(name = "social_id")
    private String socialId;

    @Column(name = "violated_count", nullable = false)
    private Long violatedCount;

    @Column(name = "is_push_enabled", nullable = false)
    private Boolean isPushEnabled;

    @Builder
    public static Member createLocal(String loginId, String password, String nickName, String phoneNumber) {
        Member member = new Member();
        member.loginId = loginId;
        member.password = password;
        member.nickName = nickName;
        member.phoneNumber = phoneNumber;
        member.role = Role.USER;
        member.uuid = UUID.randomUUID().toString();
        member.type = SocialType.LOCAL;
        member.violatedCount = 0L;
        member.isPushEnabled = true;
        return member;
    }

    @Builder(builderMethodName = "createSocialMember")
    public static Member createSocial(String socialId, SocialType socialType, String nickName, String phoneNumber) {
        Member member = new Member();
        member.socialId = socialId;
        member.type = socialType;
        member.nickName = nickName;
        member.phoneNumber = phoneNumber;
        member.role = Role.USER;
        member.uuid = UUID.randomUUID().toString();
        member.violatedCount = 0L;
        member.isPushEnabled = true;
        return member;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateProfile(String profileUrl) {
        this.profile = profileUrl;
    }

    public void updateNickName(String newNickName) {
        this.nickName = newNickName;
    }

    public void increaseViolatedCount() {
        this.violatedCount++;
    }

    public void togglePushNotification() {
        this.isPushEnabled = !this.isPushEnabled;
    }
}
