package tetoandeggens.seeyouagainbe.auth.oauth2.common.provider;

import tetoandeggens.seeyouagainbe.member.entity.Member;

public interface OAuth2UnlinkServiceProvider {
    boolean unlink(Member member);
}