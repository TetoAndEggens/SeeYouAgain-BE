package tetoandeggens.seeyouagainbe.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialType {
    LOCAL("LOCAL"),
    KAKAO("KAKAO");

    private final String type;
}