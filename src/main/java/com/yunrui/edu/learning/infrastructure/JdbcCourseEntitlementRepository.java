package com.yunrui.edu.learning.infrastructure;

import com.yunrui.edu.learning.domain.CourseEntitlement;
import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCourseEntitlementRepository implements CourseEntitlementRepository {
    private final JdbcClient jdbc;

    public JdbcCourseEntitlementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existsByOrderId(long orderId) {
        return jdbc.sql("SELECT COUNT(*) FROM edu_course_entitlement WHERE order_id = :orderId")
                .param("orderId", orderId)
                .query(Integer.class)
                .single() > 0;
    }

    @Override
    public boolean grantIfAbsent(CourseEntitlement entitlement) {
        int affected = jdbc.sql("""
                INSERT IGNORE INTO edu_course_entitlement(
                    order_id, user_id, course_id, source_event_id, granted_at)
                VALUES (:orderId, :userId, :courseId, :eventId, :grantedAt)
                """)
                .param("orderId", entitlement.orderId())
                .param("userId", entitlement.userId())
                .param("courseId", entitlement.courseId())
                .param("eventId", entitlement.sourceEventId().toString())
                .param("grantedAt", entitlement.grantedAt())
                .update();
        return affected == 1;
    }

    @Override
    public boolean existsByUserAndCourse(long userId, long courseId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM edu_course_entitlement
                WHERE user_id = :userId AND course_id = :courseId
                """)
                .param("userId", userId)
                .param("courseId", courseId)
                .query(Integer.class)
                .single() > 0;
    }
}
