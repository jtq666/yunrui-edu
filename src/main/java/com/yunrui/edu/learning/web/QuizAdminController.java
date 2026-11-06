package com.yunrui.edu.learning.web;

import com.yunrui.edu.common.id.IdGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/quiz-questions")
public class QuizAdminController {
 private final JdbcClient jdbc;private final IdGenerator ids;
 public QuizAdminController(JdbcClient jdbc,IdGenerator ids){this.jdbc=jdbc;this.ids=ids;}
 @GetMapping public List<AdminQuiz> list(@RequestParam long sectionId){requireAdmin();return jdbc.sql("SELECT id,section_id,question,option_a,option_b,option_c,option_d,correct_option,explanation,active FROM edu_quiz_question WHERE section_id=:sectionId ORDER BY id").param("sectionId",sectionId).query((rs,n)->map(rs)).list();}
 @PostMapping public AdminQuiz create(@Valid @RequestBody SaveQuiz r){requireAdmin();long exists=jdbc.sql("SELECT COUNT(*) FROM edu_course_section WHERE id=:id").param("id",r.sectionId()).query(Long.class).single();if(exists==0)throw new IllegalArgumentException("章节不存在");long id=ids.nextId();jdbc.sql("INSERT INTO edu_quiz_question(id,section_id,question,option_a,option_b,option_c,option_d,correct_option,explanation) VALUES(:id,:sectionId,:question,:a,:b,:c,:d,:correct,:explanation)").param("id",id).param("sectionId",r.sectionId()).param("question",r.question()).param("a",r.optionA()).param("b",r.optionB()).param("c",r.optionC()).param("d",r.optionD()).param("correct",r.correctOption()).param("explanation",r.explanation()).update();return find(id);}
 @DeleteMapping("/{id}") public void disable(@PathVariable long id){requireAdmin();int n=jdbc.sql("UPDATE edu_quiz_question SET active=0 WHERE id=:id").param("id",id).update();if(n==0)throw new IllegalArgumentException("测验题不存在");}
 private AdminQuiz find(long id){return jdbc.sql("SELECT id,section_id,question,option_a,option_b,option_c,option_d,correct_option,explanation,active FROM edu_quiz_question WHERE id=:id").param("id",id).query((rs,n)->map(rs)).single();}
 private AdminQuiz map(java.sql.ResultSet rs)throws java.sql.SQLException{return new AdminQuiz(rs.getLong("id"),rs.getLong("section_id"),rs.getString("question"),rs.getString("option_a"),rs.getString("option_b"),rs.getString("option_c"),rs.getString("option_d"),rs.getString("correct_option"),rs.getString("explanation"),rs.getBoolean("active"));}
 private void requireAdmin(){long id=Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());long n=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE id=:id AND role='ADMIN'").param("id",id).query(Long.class).single();if(n==0)throw new IllegalStateException("需要管理员权限");}
 public record SaveQuiz(@Positive long sectionId,@NotBlank @Size(max=500) String question,@NotBlank @Size(max=300) String optionA,@NotBlank @Size(max=300) String optionB,@NotBlank @Size(max=300) String optionC,@NotBlank @Size(max=300) String optionD,@Pattern(regexp="[ABCD]") String correctOption,@NotBlank @Size(max=1000) String explanation){}
 public record AdminQuiz(long id,long sectionId,String question,String optionA,String optionB,String optionC,String optionD,String correctOption,String explanation,boolean active){}
}
