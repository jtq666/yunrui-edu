package com.yunrui.edu.learning.web;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.common.id.IdGenerator;
import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LearningInteractionController {
 private final JdbcClient jdbc;private final UserAccessGuard guard;private final IdGenerator ids;private final CourseEntitlementRepository entitlements;
 public LearningInteractionController(JdbcClient jdbc,UserAccessGuard guard,IdGenerator ids,CourseEntitlementRepository entitlements){this.jdbc=jdbc;this.guard=guard;this.ids=ids;this.entitlements=entitlements;}

 @GetMapping("/users/{userId}/questions") public List<Question> myQuestions(@PathVariable long userId,@RequestParam long courseId){guard.require(userId);return questionQuery("WHERE q.user_id=:userId AND q.course_id=:courseId",userId,courseId);}
 @PostMapping("/users/{userId}/questions") public Question ask(@PathVariable long userId,@Valid @RequestBody AskRequest r){guard.require(userId);requireOwned(userId,r.courseId());requireSection(r.sectionId(),r.courseId());long id=ids.nextId();jdbc.sql("INSERT INTO edu_course_question(id,user_id,course_id,section_id,content) VALUES(:id,:userId,:courseId,:sectionId,:content)").param("id",id).param("userId",userId).param("courseId",r.courseId()).param("sectionId",r.sectionId()).param("content",r.content().trim()).update();return findQuestion(id);}
 @GetMapping("/admin/questions") public List<Question> openQuestions(){requireAdmin();return questionQuery("WHERE q.status='OPEN'",0,0);}
 @PostMapping("/admin/questions/{questionId}/answer") @Transactional public Question answer(@PathVariable long questionId,@Valid @RequestBody AnswerRequest r){long admin=requireAdmin();Question q=findQuestion(questionId);jdbc.sql("UPDATE edu_course_question SET status='ANSWERED',answer=:answer,answered_by=:admin,answered_at=CURRENT_TIMESTAMP(3) WHERE id=:id").param("answer",r.answer().trim()).param("admin",admin).param("id",questionId).update();jdbc.sql("INSERT INTO edu_notification(id,user_id,title,content) VALUES(:id,:userId,'课程问题已有回复',:content)").param("id",ids.nextId()).param("userId",q.userId()).param("content","你的问题“"+abbreviate(q.content(),40)+"”已有教师回复").update();return findQuestion(questionId);}

 @GetMapping("/users/{userId}/quizzes") public List<Quiz> quizzes(@PathVariable long userId,@RequestParam long sectionId){guard.require(userId);long courseId=courseOf(sectionId);requireOwned(userId,courseId);return jdbc.sql("SELECT id,section_id,question,option_a,option_b,option_c,option_d FROM edu_quiz_question WHERE section_id=:sectionId AND active=1 ORDER BY id").param("sectionId",sectionId).query((rs,n)->new Quiz(rs.getLong("id"),rs.getLong("section_id"),rs.getString("question"),List.of(rs.getString("option_a"),rs.getString("option_b"),rs.getString("option_c"),rs.getString("option_d")))).list();}
 @PostMapping("/users/{userId}/quizzes/{questionId}/submit") public QuizResult submit(@PathVariable long userId,@PathVariable long questionId,@Valid @RequestBody SubmitQuiz r){guard.require(userId);QuizAnswer a=jdbc.sql("SELECT q.correct_option,q.explanation,s.course_id FROM edu_quiz_question q JOIN edu_course_section s ON s.id=q.section_id WHERE q.id=:id AND q.active=1").param("id",questionId).query((rs,n)->new QuizAnswer(rs.getString("correct_option"),rs.getString("explanation"),rs.getLong("course_id"))).optional().orElseThrow(()->new IllegalArgumentException("测验题不存在或已停用"));requireOwned(userId,a.courseId());boolean correct=a.correctOption().equals(r.selectedOption());jdbc.sql("INSERT INTO edu_quiz_attempt(id,user_id,question_id,selected_option,correct) VALUES(:id,:userId,:questionId,:selected,:correct)").param("id",ids.nextId()).param("userId",userId).param("questionId",questionId).param("selected",r.selectedOption()).param("correct",correct).update();return new QuizResult(correct,a.correctOption(),a.explanation());}

 @GetMapping("/users/{userId}/notifications") public List<Notification> notifications(@PathVariable long userId){guard.require(userId);return jdbc.sql("SELECT id,title,content,read_flag,created_at FROM edu_notification WHERE user_id=:userId ORDER BY created_at DESC LIMIT 100").param("userId",userId).query((rs,n)->new Notification(rs.getLong("id"),rs.getString("title"),rs.getString("content"),rs.getBoolean("read_flag"),rs.getTimestamp("created_at").toInstant())).list();}
 @PostMapping("/users/{userId}/notifications/{id}/read") public void read(@PathVariable long userId,@PathVariable long id){guard.require(userId);jdbc.sql("UPDATE edu_notification SET read_flag=1 WHERE id=:id AND user_id=:userId").param("id",id).param("userId",userId).update();}

 private List<Question> questionQuery(String where,long userId,long courseId){var spec=jdbc.sql("SELECT q.id,q.user_id,q.course_id,q.section_id,q.content,q.status,q.answer,q.created_at,q.answered_at FROM edu_course_question q "+where+" ORDER BY q.created_at DESC");if(userId>0)spec=spec.param("userId",userId).param("courseId",courseId);return spec.query((rs,n)->mapQuestion(rs)).list();}
 private Question findQuestion(long id){return jdbc.sql("SELECT id,user_id,course_id,section_id,content,status,answer,created_at,answered_at FROM edu_course_question WHERE id=:id").param("id",id).query((rs,n)->mapQuestion(rs)).optional().orElseThrow(()->new IllegalArgumentException("问题不存在"));}
 private Question mapQuestion(java.sql.ResultSet rs)throws java.sql.SQLException{return new Question(rs.getLong("id"),rs.getLong("user_id"),rs.getLong("course_id"),rs.getLong("section_id"),rs.getString("content"),rs.getString("status"),rs.getString("answer"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("answered_at")==null?null:rs.getTimestamp("answered_at").toInstant());}
 private long courseOf(long sectionId){return jdbc.sql("SELECT course_id FROM edu_course_section WHERE id=:id").param("id",sectionId).query(Long.class).optional().orElseThrow(()->new IllegalArgumentException("章节不存在"));}
 private void requireOwned(long userId,long courseId){if(!entitlements.existsByUserAndCourse(userId,courseId))throw new IllegalStateException("购买课程后才能使用该功能");}
 private void requireSection(long sectionId,long courseId){long count=jdbc.sql("SELECT COUNT(*) FROM edu_course_section WHERE id=:sectionId AND course_id=:courseId").param("sectionId",sectionId).param("courseId",courseId).query(Long.class).single();if(count==0)throw new IllegalArgumentException("章节不属于当前课程");}
 private long requireAdmin(){String name=SecurityContextHolder.getContext().getAuthentication().getName();long id=Long.parseLong(name);long n=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE id=:id AND role='ADMIN'").param("id",id).query(Long.class).single();if(n==0)throw new IllegalStateException("需要管理员权限");return id;}
 private String abbreviate(String s,int max){return s.length()<=max?s:s.substring(0,max)+"…";}
 public record AskRequest(@Positive long courseId,@Positive long sectionId,@NotBlank @Size(max=1000) String content){}
 public record AnswerRequest(@NotBlank @Size(max=1000) String answer){}
 public record Question(long id,long userId,long courseId,long sectionId,String content,String status,String answer,Instant createdAt,Instant answeredAt){}
 public record Quiz(long id,long sectionId,String question,List<String> options){}
 public record SubmitQuiz(@Pattern(regexp="[ABCD]") String selectedOption){}
 private record QuizAnswer(String correctOption,String explanation,long courseId){}
 public record QuizResult(boolean correct,String correctOption,String explanation){}
 public record Notification(long id,String title,String content,boolean read,Instant createdAt){}
}
