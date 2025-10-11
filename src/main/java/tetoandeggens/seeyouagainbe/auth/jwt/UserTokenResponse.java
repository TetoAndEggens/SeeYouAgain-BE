package tetoandeggens.seeyouagainbe.auth.jwt;

public record UserTokenResponse(
        String accessToken,
        String refreshToken
) {}