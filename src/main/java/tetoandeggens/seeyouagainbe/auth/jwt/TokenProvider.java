package tetoandeggens.seeyouagainbe.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.auth.util.CookieUtil;
import tetoandeggens.seeyouagainbe.member.entity.Role;
import tetoandeggens.seeyouagainbe.global.constants.AuthConstants;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AuthErrorCode;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider {
    private final RedisTemplate<String, String> redisTemplate;

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

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰도 Claims는 유효하므로 그대로 반환
            log.info("임시 소셜 토큰 만료 (payload는 반환됨)");
            return e.getClaims();
        } catch (JwtException e) {
            log.error("JWT 파싱 실패: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public UserTokenResponse createLoginToken(String uuid, Role role) {
        String accessToken = createAccessToken(uuid, role.getRole());
        String refreshToken = createRefreshToken(uuid, role.getRole());
        saveRefreshToken(uuid, refreshToken);
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
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000)) // 10분
                .signWith(secretKey, Jwts.SIG.HS512)
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
            throw new CustomException(AuthErrorCode.INVALID_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
            throw new CustomException(AuthErrorCode.UNSUPPORTED_JWT);
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임 문자열이 비어 있습니다.");
            throw new CustomException(AuthErrorCode.EMPTY_JWT_CLAIMS);
        }
    }

    public Authentication getAuthenticationByAccessToken(String accessToken) {
        Claims claims = getClaimsFromToken(accessToken);
        String uuid = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        CustomUserDetails customUserDetails = CustomUserDetails.fromClaims(uuid, role);
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
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public String resolveAccessToken(HttpServletRequest request) {
        return CookieUtil.resolveCookieValue(request, AuthConstants.ACCESS_TOKEN_COOKIE_NAME);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return CookieUtil.resolveCookieValue(request, AuthConstants.REFRESH_TOKEN_COOKIE_NAME);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        CookieUtil.setAccessTokenCookie(response, accessToken, accessTokenExpirationMs / 1000);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        CookieUtil.setRefreshTokenCookie(response, refreshToken, refreshTokenExpirationMs / 1000);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        CookieUtil.deleteAccessTokenCookie(response);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        CookieUtil.deleteRefreshTokenCookie(response);
    }

    public void deleteRefreshToken(String uuid) {
        redisTemplate.delete(uuid);
    }

    public String reissueAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        Claims claims = getClaimsFromToken(refreshToken);
        String uuid = claims.getSubject();
        String role = claims.get(AuthConstants.ROLE_CLAIM, String.class);

        String storedRefreshToken = redisTemplate.opsForValue().get(uuid);
        if (storedRefreshToken == null) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        if (!storedRefreshToken.equals(refreshToken)) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        return createAccessToken(uuid, role);
    }

    private void saveRefreshToken(String uuid, String refreshToken) {
        redisTemplate.opsForValue().set(
                uuid,
                refreshToken,
                refreshTokenExpirationMs,
                TimeUnit.MILLISECONDS
        );
    }
}
