package com.yunrui.edu.student;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
 private final JdbcClient jdbc;
 public AdminDashboardController(JdbcClient jdbc){this.jdbc=jdbc;}
 @GetMapping public Dashboard dashboard(){requireAdmin();long users=count("SELECT COUNT(*) FROM edu_user_account WHERE status='ACTIVE'");long courses=count("SELECT COUNT(*) FROM edu_course WHERE status='PUBLISHED'");long paidOrders=count("SELECT COUNT(*) FROM edu_purchase_order WHERE status IN ('PAID','REFUNDED')");long learners=count("SELECT COUNT(DISTINCT user_id) FROM edu_course_entitlement");long pendingRefunds=count("SELECT COUNT(*) FROM edu_refund_request WHERE status='PENDING'");long openQuestions=count("SELECT COUNT(*) FROM edu_course_question WHERE status='OPEN'");BigDecimal revenue=jdbc.sql("SELECT COALESCE(SUM(payable_amount),0) FROM edu_purchase_order WHERE status='PAID'").query(BigDecimal.class).single();List<CourseSale> top=jdbc.sql("SELECT c.id,c.title,COUNT(o.id) sales,COALESCE(SUM(o.payable_amount),0) revenue FROM edu_course c LEFT JOIN edu_purchase_order o ON o.course_id=c.id AND o.status='PAID' GROUP BY c.id,c.title ORDER BY sales DESC,c.id LIMIT 5").query((rs,n)->new CourseSale(rs.getLong("id"),rs.getString("title"),rs.getLong("sales"),rs.getBigDecimal("revenue"))).list();return new Dashboard(users,courses,paidOrders,learners,pendingRefunds,openQuestions,revenue,top);}
 private long count(String sql){return jdbc.sql(sql).query(Long.class).single();}
 private void requireAdmin(){long id=Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());long n=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE id=:id AND role='ADMIN'").param("id",id).query(Long.class).single();if(n==0)throw new IllegalStateException("需要管理员权限");}
 public record Dashboard(long users,long publishedCourses,long paidOrders,long learners,long pendingRefunds,long openQuestions,BigDecimal revenue,List<CourseSale> topCourses){}
 public record CourseSale(long courseId,String title,long sales,BigDecimal revenue){}
}
