package com.yunrui.edu.points;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class JdbcPointsRecordRepository implements PointsRecordRepository {
    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;

    public JdbcPointsRecordRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean append(String eventId, long userId, int points, String reason, String season) {
        return jdbc.sql("""
                INSERT IGNORE INTO edu_points_record(event_id, user_id, points, reason, season)
                VALUES (:eventId, :userId, :points, :reason, :season)
                """)
                .param("eventId", eventId)
                .param("userId", userId)
                .param("points", points)
                .param("reason", reason)
                .param("season", season)
                .update() == 1;
    }

    @Override
    public List<PointsRecord> findPending(int limit) {
        return jdbc.sql("""
                SELECT id, event_id, user_id, points, reason, season
                FROM edu_points_record
                WHERE ranked = 0
                ORDER BY id
                LIMIT :limit
                """)
                .param("limit", limit)
                .query((rs, rowNum) -> new PointsRecord(
                        rs.getLong("id"), rs.getString("event_id"),
                        rs.getLong("user_id"), rs.getInt("points"),
                        rs.getString("reason"), rs.getString("season")))
                .list();
    }

    @Override
    public void markRanked(long id) {
        jdbc.sql("UPDATE edu_points_record SET ranked = 1 WHERE id = :id")
                .param("id", id)
                .update();
    }

    @Override
    @Transactional
    public void replaceArchive(String season, List<PointsBoard.Entry> entries) {
        jdbc.sql("DELETE FROM edu_points_board_archive WHERE season = :season")
                .param("season", season)
                .update();
        jdbcTemplate.batchUpdate("""
                        INSERT INTO edu_points_board_archive(season, user_id, points, rank_no)
                        VALUES (?, ?, ?, ?)
                        """,
                entries,
                entries.size(),
                (ps, entry) -> {
                    ps.setString(1, season);
                    ps.setLong(2, entry.userId());
                    ps.setInt(3, entry.points());
                    ps.setLong(4, entry.rank());
                });
    }
}
