package tetoandeggens.seeyouagainbe.auth.oauth2.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.oauth2.common.provider.OAuth2Provider;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenExtractor {
    private final ApplicationContext applicationContext;

    public String extractRefreshToken(OAuth2AuthenticationToken oAuth2Token, OAuth2Provider provider) {
        if (!provider.isRequiresRefreshToken()) {
            log.debug("[OAuth2TokenExtractor] {} 는 RefreshToken 불필요", provider.getRegistrationId());
            return null;
        }

        try {
            OAuth2AuthorizedClientService authorizedClientService = applicationContext.getBean(OAuth2AuthorizedClientService.class);

            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oAuth2Token.getAuthorizedClientRegistrationId(),
                    oAuth2Token.getName()
            );

            if (authorizedClient == null) {
                log.warn("[OAuth2TokenExtractor] {} OAuth2AuthorizedClient가 null", provider.getRegistrationId());
                return null;
            }

            OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
            if (refreshToken != null) {
                log.info("[OAuth2TokenExtractor] {} RefreshToken 획득 성공", provider.getRegistrationId());
                return refreshToken.getTokenValue();
            } else {
                log.warn("[OAuth2TokenExtractor] {} RefreshToken이 null", provider.getRegistrationId());
                return null;
            }

        } catch (Exception e) {
            log.error("[OAuth2TokenExtractor] {} RefreshToken 조회 실패", provider.getRegistrationId(), e);
            return null;
        }
    }
}