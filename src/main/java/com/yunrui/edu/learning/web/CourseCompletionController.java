package com.yunrui.edu.learning.web;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.common.id.IdGenerator;
import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/courses/{courseId}")
public class CourseCompletionController {
 private final JdbcClient jdbc;private final UserAccessGuard guard;private final CourseEntitlementRepository entitlements;private final IdGenerator ids;
 public CourseCompletionController(JdbcClient jdbc,UserAccessGuard guard,CourseEntitlementRepository entitlements,IdGenerator ids){this.jdbc=jdbc;this.guard=guard;this.entitlements=entitlements;this.ids=ids;}
 @GetMapping("/completion") public Completion completion(@PathVariable long userId,@PathVariable long courseId){guard.require(userId);requireOwned(userId,courseId);Stats s=jdbc.sql("SELECT COUNT(*) total,COALESCE(SUM(CASE WHEN p.completed=1 THEN 1 ELSE 0 END),0) completed,COALESCE(AVG(CASE WHEN p.duration_seconds>0 THEN LEAST(p.position_seconds*100.0/p.duration_seconds,100) ELSE 0 END),0) percentage FROM edu_course_section s LEFT JOIN edu_learning_progress p ON p.section_id=s.id AND p.user_id=:userId AND p.course_id=:courseId WHERE s.course_id=:courseId").param("userId",userId).param("courseId",courseId).query((rs,n)->new Stats(rs.getInt("total"),rs.getInt("completed"),rs.getDouble("percentage"))).single();Certificate certificate=findCertificate(userId,courseId);return new Completion(s.total(),s.completed(),Math.round(s.percentage()*10)/10.0,s.total()>0&&s.total()==s.completed(),certificate);}
 @PostMapping("/certificate") public Certificate issue(@PathVariable long userId,@PathVariable long courseId){Completion c=completion(userId,courseId);if(!c.eligible())throw new IllegalStateException("完成全部章节后才能领取结课证明");Certificate old=findCertificate(userId,courseId);if(old!=null)return old;String code="YR-"+UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase();jdbc.sql("INSERT INTO edu_course_certificate(id,user_id,course_id,certificate_code) VALUES(:id,:userId,:courseId,:code)").param("id",ids.nextId()).param("userId",userId).param("courseId",courseId).param("code",code).update();return findCertificate(userId,courseId);}
 @GetMapping("/certificate") public Certificate certificate(@PathVariable long userId,@PathVariable long courseId){guard.require(userId);Certificate c=findCertificate(userId,courseId);if(c==null)throw new IllegalArgumentException("尚未领取结课证明");return c;}
 private Certificate findCertificate(long userId,long courseId){return jdbc.sql("SELECT certificate_code,issued_at FROM edu_course_certificate WHERE user_id=:userId AND course_id=:courseId").param("userId",userId).param("courseId",courseId).query((rs,n)->new Certificate(rs.getString("certificate_code"),rs.getTimestamp("issued_at").toInstant())).optional().orElse(null);}
 private void requireOwned(long userId,long courseId){if(!entitlements.existsByUserAndCourse(userId,courseId))throw new IllegalStateException("尚未获得课程学习权限");}
 private record Stats(int total,int completed,double percentage){}
 public record Completion(int totalSections,int completedSections,double percentage,boolean eligible,Certificate certificate){}
 public record Certificate(String code,Instant issuedAt){}
}
