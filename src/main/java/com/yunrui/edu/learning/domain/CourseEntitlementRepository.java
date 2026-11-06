package com.yunrui.edu.learning.domain;

public interface CourseEntitlementRepository {
    boolean existsByOrderId(long orderId);

    boolean grantIfAbsent(CourseEntitlement entitlement);

    boolean existsByUserAndCourse(long userId, long courseId);
}
