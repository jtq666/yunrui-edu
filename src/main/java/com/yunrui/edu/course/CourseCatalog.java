package com.yunrui.edu.course;

import java.util.List;
import java.util.Optional;

public interface CourseCatalog {
    Optional<Course> findPublished(long courseId);

    List<Course> searchPublished(String keyword, String category, int limit);

    PageResult<Course> pagePublished(String keyword, String category, int page, int pageSize);

    List<String> categories();

    record PageResult<T>(List<T> records, long total, int page, int pageSize) {}
}
