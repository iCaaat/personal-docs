package com.luckyby.personaldocs.auth;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Redis 登录失败计数器：5 次失败锁定，锁定时间按轮次指数增长。 */
@Service
public class LoginAttemptService {
  private static final int MAX_FAILURES_PER_ROUND = 5;
  private static final long INITIAL_LOCK_SECONDS = 30;
  private static final long MAX_LOCK_SECONDS = Duration.ofHours(2).toSeconds();
  private static final Duration STATE_TTL = Duration.ofHours(24);
  private final StringRedisTemplate redisTemplate;

  public LoginAttemptService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void checkNotLocked(String username) {
    Long remaining = redisTemplate.getExpire(lockKey(username));
    if (remaining != null && remaining > 0)
      throw new LoginLockedException(remaining);
  }

  public void recordFailure(String username) {
    String key = stateKey(username);
    long failures = redisTemplate.opsForHash().increment(key, "failures", 1);
    redisTemplate.expire(key, STATE_TTL);
    if (failures < MAX_FAILURES_PER_ROUND)
      return;

    long previousLevel = parseLong(redisTemplate.opsForHash().get(key, "lockLevel"));
    long level = previousLevel + 1;
    long lockSeconds = Math.min(MAX_LOCK_SECONDS, INITIAL_LOCK_SECONDS * (1L << Math.min(level - 1, 8)));
    redisTemplate.opsForHash().put(key, "failures", "0");
    redisTemplate.opsForHash().put(key, "lockLevel", Long.toString(level));
    redisTemplate.opsForValue().set(lockKey(username), "1", Duration.ofSeconds(lockSeconds));
  }

  public void clear(String username) {
    redisTemplate.delete(lockKey(username));
    redisTemplate.delete(stateKey(username));
  }
  private long parseLong(Object value) {
    try {
      return value == null ? 0L : Long.parseLong(value.toString());
    } catch (NumberFormatException ignored) {
      return 0L;
    }
  }
  private String lockKey(String username) { return "auth:login:lock:" + username; }
  private String stateKey(String username) { return "auth:login:state:" + username; }
  public static class LoginLockedException extends RuntimeException { private final long remainingSeconds; public LoginLockedException(long remainingSeconds) { this.remainingSeconds = remainingSeconds; } public long remainingSeconds() { return remainingSeconds; } }
}
