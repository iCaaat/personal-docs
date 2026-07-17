package com.luckyby.personaldocs.auth;

import com.luckyby.personaldocs.auth.LoginAttemptService.LoginLockedException;
import com.luckyby.personaldocs.security.JwtService;
import com.luckyby.personaldocs.user.AppUser;
import com.luckyby.personaldocs.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final TokenSessionService tokenSessionService;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttemptService,
                          TokenSessionService tokenSessionService,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.tokenSessionService = tokenSessionService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public TokenSessionService.TokenPair login(@Valid @RequestBody LoginRequest request) {
        loginAttemptService.checkNotLocked(request.username());
        AppUser user = userRepository.findByUsername(request.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(request.username());
            // 第五次失败会在 recordFailure 中创建锁定键，此处立即返回锁定提示。
            loginAttemptService.checkNotLocked(request.username());
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        loginAttemptService.clear(user.getUsername());
        return tokenSessionService.create(user);
    }

    @PostMapping("/refresh")
    public TokenSessionService.TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        JwtService.TokenPayload payload;
        try {
            payload = jwtService.parse(request.refreshToken());
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        AppUser user = userRepository.findByUsername(payload.username()).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return tokenSessionService.rotate(request.refreshToken(), user);
    }

    @ExceptionHandler(LoginLockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LoginLockedException exception) {
        long seconds = exception.remainingSeconds();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse("登录失败次数过多，请在 " + seconds + " 秒后重试"));
    }

    public record LoginRequest(@NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 100) String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ErrorResponse(String message) {
    }
}
