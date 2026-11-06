package com.yunrui.edu.learning.progress;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcLearningProgressRepository implements LearningProgressRepository {
    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;

    public JdbcLearningProgressRepository(JdbcTemplate jdbcTemplate, JdbcClient jdbcClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void upsertBatch(List<ProgressSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                        INSERT INTO edu_learning_progress(
                            user_id, course_id, section_id, position_seconds,
                            duration_seconds, completed, reported_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            position_seconds = VALUES(position_seconds),
                            duration_seconds = VALUES(duration_seconds),
                            completed = completed OR VALUES(completed),
                            reported_at = VALUES(reported_at)
                        """,
                snapshots,
                snapshots.size(),
                (ps, snapshot) -> {
                    ps.setLong(1, snapshot.userId());
                    ps.setLong(2, snapshot.courseId());
                    ps.setLong(3, snapshot.sectionId());
                    ps.setInt(4, snapshot.positionSeconds());
                    ps.setInt(5, snapshot.durationSeconds());
                    ps.setBoolean(6, snapshot.completed());
                    ps.setTimestamp(7, Timestamp.from(snapshot.reportedAt()));
                });
    }

    @Override
    public List<ProgressSnapshot> findByUserAndCourse(long userId, long courseId) {
        return jdbcClient.sql("""
                SELECT user_id, course_id, section_id, position_seconds,
                       duration_seconds, reported_at
                FROM edu_learning_progress
                WHERE user_id = :userId AND course_id = :courseId
                ORDER BY section_id
                """)
                .param("userId", userId)
                .param("courseId", courseId)
                .query((rs, rowNum) -> new ProgressSnapshot(
                        rs.getLong("user_id"),
                        rs.getLong("course_id"),
                        rs.getLong("section_id"),
                        rs.getInt("position_seconds"),
                        rs.getInt("duration_seconds"),
                        rs.getTimestamp("reported_at").toInstant()))
                .list();
    }

    @Override
    public boolean sectionBelongsToCourse(long sectionId, long courseId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM edu_course_section WHERE id=:sectionId AND course_id=:courseId")
                .param("sectionId", sectionId)
                .param("courseId", courseId)
                .query(Long.class)
                .single() > 0;
    }
}
