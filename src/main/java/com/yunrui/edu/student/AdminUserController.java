package com.yunrui.edu.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
 private final JdbcClient jdbc;
 public AdminUserController(JdbcClient jdbc){this.jdbc=jdbc;}
 @GetMapping public UserPage list(@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){requireAdmin();String word=keyword==null?"":keyword.trim();int p=Math.max(page,1),size=Math.min(Math.max(pageSize,1),100);long total=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE :word='' OR username LIKE CONCAT('%',:word,'%') OR nickname LIKE CONCAT('%',:word,'%')").param("word",word).query(Long.class).single();List<UserItem> records=jdbc.sql("SELECT id,username,nickname,email,status,role,created_at FROM edu_user_account WHERE :word='' OR username LIKE CONCAT('%',:word,'%') OR nickname LIKE CONCAT('%',:word,'%') ORDER BY created_at DESC LIMIT :size OFFSET :offset").param("word",word).param("size",size).param("offset",(p-1)*size).query((rs,n)->new UserItem(rs.getLong("id"),rs.getString("username"),rs.getString("nickname"),rs.getString("email"),rs.getString("status"),rs.getString("role"),rs.getTimestamp("created_at").toInstant())).list();return new UserPage(records,total,p,size);}
 @PutMapping("/{userId}") public UserItem update(@PathVariable long userId,@Valid @RequestBody UpdateUser r){long admin=requireAdmin();if(userId==admin&&(!"ACTIVE".equals(r.status())||!"ADMIN".equals(r.role())))throw new IllegalStateException("不能停用自己或取消自己的管理员角色");int n=jdbc.sql("UPDATE edu_user_account SET status=:status,role=:role WHERE id=:id").param("status",r.status()).param("role",r.role()).param("id",userId).update();if(n==0)throw new IllegalArgumentException("用户不存在");return find(userId);}
 private UserItem find(long id){return jdbc.sql("SELECT id,username,nickname,email,status,role,created_at FROM edu_user_account WHERE id=:id").param("id",id).query((rs,n)->new UserItem(rs.getLong("id"),rs.getString("username"),rs.getString("nickname"),rs.getString("email"),rs.getString("status"),rs.getString("role"),rs.getTimestamp("created_at").toInstant())).single();}
 private long requireAdmin(){long id=Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());long n=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE id=:id AND role='ADMIN' AND status='ACTIVE'").param("id",id).query(Long.class).single();if(n==0)throw new IllegalStateException("需要管理员权限");return id;}
 public record UpdateUser(@Pattern(regexp="ACTIVE|DISABLED") String status,@Pattern(regexp="STUDENT|ADMIN") String role){}
 public record UserItem(long id,String username,String nickname,String email,String status,String role,Instant createdAt){}
 public record UserPage(List<UserItem> records,long total,int page,int pageSize){}
}
