package com.yunrui.edu.course;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCourseCatalog implements CourseCatalog {
    private final JdbcClient jdbc;

    public JdbcCourseCatalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Course> findPublished(long courseId) {
        return jdbc.sql("""
                SELECT id, title, category, teacher_name, price, status, description
                FROM edu_course
                WHERE id = :id AND status = 'PUBLISHED'
                """)
                .param("id", courseId)
                .query((rs, rowNum) -> mapCourse(rs.getLong("id"),
                        rs.getString("title"), rs.getString("category"),
                        rs.getString("teacher_name"), rs.getBigDecimal("price"),
                        rs.getString("status"), rs.getString("description")))
                .optional();
    }

    @Override
    public List<Course> searchPublished(String keyword, String category, int limit) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeCategory = category == null ? "" : category.trim();
        return jdbc.sql("""
                SELECT id, title, category, teacher_name, price, status, description
                FROM edu_course
                WHERE status = 'PUBLISHED'
                  AND (:keyword = '' OR title LIKE CONCAT('%', :keyword, '%'))
                  AND (:category = '' OR category = :category)
                ORDER BY id
                LIMIT :limit
                """)
                .param("keyword", safeKeyword)
                .param("category", safeCategory)
                .param("limit", Math.min(Math.max(limit, 1), 100))
                .query((rs, rowNum) -> mapCourse(rs.getLong("id"),
                        rs.getString("title"), rs.getString("category"),
                        rs.getString("teacher_name"), rs.getBigDecimal("price"),
                        rs.getString("status"), rs.getString("description")))
                .list();
    }

    @Override
    public PageResult<Course> pagePublished(String keyword, String category, int page, int pageSize) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeCategory = category == null ? "" : category.trim();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        long total = jdbc.sql("""
                SELECT COUNT(*) FROM edu_course
                WHERE status = 'PUBLISHED'
                  AND (:keyword = '' OR title LIKE CONCAT('%', :keyword, '%') OR description LIKE CONCAT('%', :keyword, '%'))
                  AND (:category = '' OR category = :category)
                """).param("keyword", safeKeyword).param("category", safeCategory)
                .query(Long.class).single();
        List<Course> records = jdbc.sql("""
                SELECT id, title, category, teacher_name, price, status, description
                FROM edu_course
                WHERE status = 'PUBLISHED'
                  AND (:keyword = '' OR title LIKE CONCAT('%', :keyword, '%') OR description LIKE CONCAT('%', :keyword, '%'))
                  AND (:category = '' OR category = :category)
                ORDER BY id
                LIMIT :size OFFSET :offset
                """).param("keyword", safeKeyword).param("category", safeCategory)
                .param("size", safeSize).param("offset", (safePage - 1) * safeSize)
                .query((rs, rowNum) -> mapCourse(rs.getLong("id"), rs.getString("title"),
                        rs.getString("category"), rs.getString("teacher_name"), rs.getBigDecimal("price"),
                        rs.getString("status"), rs.getString("description"))).list();
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public List<String> categories() {
        return jdbc.sql("SELECT DISTINCT category FROM edu_course WHERE status = 'PUBLISHED' ORDER BY category")
                .query(String.class).list();
    }

    private Course mapCourse(long id,
                             String title,
                             String category,
                             String teacherName,
                             java.math.BigDecimal price,
                             String status,
                             String description) {
        List<Course.Section> sections = jdbc.sql("""
                SELECT id, title, sort_no, duration_seconds, video_file
                FROM edu_course_section
                WHERE course_id = :courseId
                ORDER BY sort_no
                """)
                .param("courseId", id)
                .query((rs, rowNum) -> new Course.Section(
                        rs.getLong("id"), rs.getString("title"),
                        rs.getInt("sort_no"), rs.getInt("duration_seconds"), rs.getString("video_file") != null))
                .list();
        return new Course(id, title, category, teacherName, price, status, description, sections);
    }
}
