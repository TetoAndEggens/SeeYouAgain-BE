package tetoandeggens.seeyouagainbe.auth.oauth2.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.dto.BaseOAuth2Attributes;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.dto.CustomOAuth2User;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.dto.OAuth2Attributes;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        BaseOAuth2Attributes attributes = OAuth2Attributes.of(
                registrationId,
                userNameAttributeName,
                oAuth2User.getAttributes()
        );

        return new CustomOAuth2User(attributes);
    }
}
