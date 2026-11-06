package com.yunrui.edu.learning.web;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import com.yunrui.edu.auth.UserAccessGuard;

@RestController
@RequestMapping("/api/users/{userId}/courses")
public class LearningController {
    private final JdbcClient jdbc;
    private final UserAccessGuard accessGuard;

    public LearningController(JdbcClient jdbc, UserAccessGuard accessGuard) {
        this.jdbc = jdbc;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<CourseEntitlementView> list(@PathVariable long userId) {
        accessGuard.require(userId);
        return jdbc.sql("""
                SELECT order_id, course_id, granted_at
                FROM edu_course_entitlement
                WHERE user_id = :userId
                ORDER BY granted_at DESC
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new CourseEntitlementView(
                        rs.getLong("order_id"),
                        rs.getLong("course_id"),
                        rs.getTimestamp("granted_at").toInstant()))
                .list();
    }

    public record CourseEntitlementView(long orderId, long courseId, Instant grantedAt) {
    }
}
