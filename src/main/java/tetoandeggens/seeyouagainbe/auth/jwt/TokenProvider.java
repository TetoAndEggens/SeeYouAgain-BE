package tetoandeggens.seeyouagainbe.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.member.entity.Role;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenExpirationSec() {
        return accessTokenExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationSec() {
        return refreshTokenExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    // 로그인 토큰 쌍 생성 (AccessToken + RefreshToken)
    public UserTokenResponse createLoginToken(String uuid, Role role) {
        String accessToken = createAccessToken(uuid, role.getRole());
        String refreshToken = createRefreshToken(uuid, role.getRole());
        return new UserTokenResponse(accessToken, refreshToken);
    }

    public String createAccessToken(String uuid, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(uuid)
                .claim(AuthConstants.ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String createRefreshToken(String uuid, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(uuid)
                .claim(AuthConstants.ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String createSocialTempToken(String provider, String profileImageUrl, String tempUuid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("provider", provider);
        claims.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        claims.put("tempUuid", tempUuid);
        claims.put("type", "social_temp");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (SecurityException | MalformedJwtException e) {
            throw new CustomException(INVALID_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            throw new CustomException(EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(UNSUPPORTED_JWT);
        } catch (IllegalArgumentException e) {
            throw new CustomException(EMPTY_JWT_CLAIMS);
        }
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw new CustomException(INVALID_TOKEN);
        }
    }

    public Authentication getAuthenticationByAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken);
        String uuid = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        CustomUserDetails customUserDetails = CustomUserDetails.fromClaims(uuid, role);
        return new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );
    }
}
