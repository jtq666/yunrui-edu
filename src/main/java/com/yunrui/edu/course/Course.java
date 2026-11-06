package com.yunrui.edu.course;

import java.math.BigDecimal;
import java.util.List;

public record Course(
        long id,
        String title,
        String category,
        String teacherName,
        BigDecimal price,
        String status,
        String description,
        List<Section> sections
) {
    public record Section(long id, String title, int sortNo, int durationSeconds, boolean videoAvailable) {
    }
}
