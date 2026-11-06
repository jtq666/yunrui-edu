package com.yunrui.edu.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(JdbcClient jdbc, PasswordEncoder encoder, JwtService jwtService) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Transactional
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        try {
            jdbc.sql("""
                    INSERT INTO edu_user_account(username, password_hash, nickname)
                    VALUES (:username, :password, :nickname)
                    """).param("username", request.username())
                    .param("password", encoder.encode(request.password()))
                    .param("nickname", request.nickname()).update();
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("用户名已存在");
        }
        long id = jdbc.sql("SELECT id FROM edu_user_account WHERE username = :username")
                .param("username", request.username()).query(Long.class).single();
        return new TokenResponse(id, jwtService.create(id));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Account account = jdbc.sql("""
                SELECT id, password_hash FROM edu_user_account
                WHERE username = :username AND status = 'ACTIVE'
                """).param("username", request.username())
                .query((rs, rowNum) -> new Account(rs.getLong("id"), rs.getString("password_hash")))
                .optional().orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!encoder.matches(request.password(), account.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return new TokenResponse(account.id(), jwtService.create(account.id()));
    }

    public record RegisterRequest(@NotBlank @Size(max = 64) String username,
                                  @NotBlank @Size(min = 6, max = 72) String password,
                                  @NotBlank @Size(max = 64) String nickname) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(long userId, String token) {}
    private record Account(long id, String passwordHash) {}
}
