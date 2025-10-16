package tetoandeggens.seeyouagainbe.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialType {
    GENERAL("GENERAL"),
    KAKAO("KAKAO");

    private final String type;
}