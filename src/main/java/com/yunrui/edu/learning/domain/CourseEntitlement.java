package com.yunrui.edu.learning.domain;

import java.time.Instant;
import java.util.UUID;

public record CourseEntitlement(
        long orderId,
        long userId,
        long courseId,
        UUID sourceEventId,
        Instant grantedAt
) {
}
