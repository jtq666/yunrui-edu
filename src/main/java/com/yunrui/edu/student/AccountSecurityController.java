package com.yunrui.edu.student;

import com.yunrui.edu.auth.UserAccessGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/security")
public class AccountSecurityController {
 private final JdbcClient jdbc;private final UserAccessGuard guard;private final PasswordEncoder encoder;
 public AccountSecurityController(JdbcClient jdbc,UserAccessGuard guard,PasswordEncoder encoder){this.jdbc=jdbc;this.guard=guard;this.encoder=encoder;}
 @PostMapping("/password") public void password(@PathVariable long userId,@Valid @RequestBody ChangePassword r){guard.require(userId);String hash=jdbc.sql("SELECT password_hash FROM edu_user_account WHERE id=:id").param("id",userId).query(String.class).optional().orElseThrow(()->new IllegalArgumentException("用户不存在"));if(!encoder.matches(r.oldPassword(),hash))throw new IllegalArgumentException("原密码错误");if(r.oldPassword().equals(r.newPassword()))throw new IllegalArgumentException("新密码不能与原密码相同");jdbc.sql("UPDATE edu_user_account SET password_hash=:hash WHERE id=:id").param("hash",encoder.encode(r.newPassword())).param("id",userId).update();}
 public record ChangePassword(@NotBlank String oldPassword,@NotBlank @Size(min=8,max=72) String newPassword){}
}
