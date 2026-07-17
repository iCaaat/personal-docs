package com.luckyby.personaldocs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 负责签发和校验访问令牌、刷新令牌。 */
@Service
public class JwtService {
  private final SecretKey signingKey;
  private final Duration accessExpiration;
  private final Duration refreshExpiration;

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.access-expiration-minutes}") long accessMinutes,
                    @Value("${app.jwt.refresh-expiration-hours}") long refreshHours) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessExpiration = Duration.ofMinutes(accessMinutes);
    this.refreshExpiration = Duration.ofHours(refreshHours);
  }

  public IssuedToken createAccessToken(String username, String role) { return create(username, role, TokenType.ACCESS, accessExpiration); }
  public IssuedToken createRefreshToken(String username) { return create(username, null, TokenType.REFRESH, refreshExpiration); }
  public TokenPayload parse(String token) {
    Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    return new TokenPayload(claims.getSubject(), claims.get("role", String.class), TokenType.valueOf(claims.get("type", String.class)), claims.getId());
  }
  public Duration refreshExpiration() { return refreshExpiration; }

  private IssuedToken create(String username, String role, TokenType type, Duration expiration) {
    String tokenId = UUID.randomUUID().toString();
    Date now = new Date();
    var builder = Jwts.builder().subject(username).id(tokenId).claim("type", type.name()).issuedAt(now).expiration(new Date(now.getTime() + expiration.toMillis()));
    if (role != null) builder.claim("role", role);
    return new IssuedToken(builder.signWith(signingKey).compact(), tokenId);
  }

  public enum TokenType { ACCESS, REFRESH }
  public record IssuedToken(String value, String tokenId) { }
  public record TokenPayload(String username, String role, TokenType type, String tokenId) { }
}
