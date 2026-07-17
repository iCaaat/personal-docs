package com.luckyby.personaldocs.auth;

import com.luckyby.personaldocs.security.JwtService;
import com.luckyby.personaldocs.security.JwtService.TokenType;
import com.luckyby.personaldocs.user.AppUser;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 使用 Redis 保存刷新令牌会话，实现刷新令牌轮换和即时失效。 */
@Service
public class TokenSessionService {
  private final JwtService jwtService;
  private final StringRedisTemplate redisTemplate;
  public TokenSessionService(JwtService jwtService, StringRedisTemplate redisTemplate) { this.jwtService = jwtService; this.redisTemplate = redisTemplate; }
  public TokenPair create(AppUser user) {
    JwtService.IssuedToken access = jwtService.createAccessToken(user.getUsername(), user.getRole());
    JwtService.IssuedToken refresh = jwtService.createRefreshToken(user.getUsername());
    redisTemplate.opsForValue().set(refreshKey(refresh.tokenId()), user.getUsername(), jwtService.refreshExpiration());
    return new TokenPair(access.value(), refresh.value(), user.getUsername());
  }
  public TokenPair rotate(String rawRefreshToken, AppUser user) {
    JwtService.TokenPayload payload;
    try { payload = jwtService.parse(rawRefreshToken); } catch (Exception exception) { throw unauthorized(); }
    if (payload.type() != TokenType.REFRESH || !Objects.equals(payload.username(), user.getUsername())) throw unauthorized();
    if (!Boolean.TRUE.equals(redisTemplate.delete(refreshKey(payload.tokenId())))) throw unauthorized();
    return create(user);
  }
  private String refreshKey(String tokenId) { return "auth:refresh:" + tokenId; }
  private ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期"); }
  public record TokenPair(String accessToken, String refreshToken, String username) { }
}
