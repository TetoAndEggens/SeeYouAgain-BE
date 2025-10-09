package tetoandeggens.seeyouagainbe.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final RedisTemplate<String, String> redisTemplate;

    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMs,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.redisTemplate = redisTemplate;
    }

    public String createAccessToken(String userId, String role) {
        return createToken(userId, role, accessTokenExpirationMs);
    }

    public String createRefreshToken(String userId, String role) {
        String refreshToken = createToken(userId, role, refreshTokenExpirationMs);
        saveRefreshToken(userId, refreshToken);
        return refreshToken;
    }

    public UserTokenResponse createLoginToken(String userId, String role) {
        String accessToken = createAccessToken(userId, role);
        String refreshToken = createRefreshToken(userId, role);
        return new UserTokenResponse(accessToken, refreshToken);
    }

    private String createToken(String userId, String role, long expirationMs) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim(AuthConstants.ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임 문자열이 비어 있습니다.");
        }
        return false;
    }

    public Authentication getAuthenticationByAccessToken(String accessToken) {
        Claims claims = getClaimsFromToken(accessToken);
        String userId = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        CustomUserDetails customUserDetails = CustomUserDetails.fromClaims(userId, role);
        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.info("토큰 만료");
            return e.getClaims();
        } catch (JwtException e) {
            log.error("잘못된 JWT 토큰입니다. {}", e.getMessage());
            throw new IllegalArgumentException("잘못된 JWT 토큰입니다.");
        }
    }

    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearerToken.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(userId);
    }

    private void saveRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                userId,
                refreshToken,
                refreshTokenExpirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    public String reissueAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        Claims claims = getClaimsFromToken(refreshToken);
        String userId = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        String storedRefreshToken = redisTemplate.opsForValue().get(userId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Redis에 저장된 Refresh Token과 일치하지 않습니다.");
        }

        return createAccessToken(userId, role);
    }
}
