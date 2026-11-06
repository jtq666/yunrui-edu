package com.yunrui.edu.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public JdbcOutboxRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(OutboxMessage message) {
        jdbc.sql("""
                INSERT INTO edu_outbox_message(
                    id, event_type, aggregate_id, payload, status,
                    retry_count, next_retry_at, created_at)
                VALUES (:id, :eventType, :aggregateId, :payload, :status,
                        :retryCount, :nextRetryAt, :createdAt)
                """)
                .param("id", message.getId().toString())
                .param("eventType", message.getEventType())
                .param("aggregateId", message.getEvent().orderId())
                .param("payload", writeJson(message.getEvent()))
                .param("status", message.getStatus().name())
                .param("retryCount", message.getRetryCount())
                .param("nextRetryAt", message.getNextRetryAt())
                .param("createdAt", message.getCreatedAt())
                .update();
    }

    @Override
    public List<OutboxMessage> findPublishable(Instant now, int limit) {
        return jdbc.sql("""
                SELECT id, event_type, payload, status, retry_count,
                       next_retry_at, last_error, created_at
                FROM edu_outbox_message
                WHERE status = 'PENDING' AND next_retry_at <= :now
                ORDER BY created_at
                LIMIT :limit
                """)
                .param("now", now)
                .param("limit", limit)
                .query((rs, rowNum) -> OutboxMessage.restore(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("event_type"),
                        readEvent(rs.getString("payload")),
                        rs.getTimestamp("created_at").toInstant(),
                        OutboxStatus.valueOf(rs.getString("status")),
                        rs.getInt("retry_count"),
                        rs.getTimestamp("next_retry_at").toInstant(),
                        rs.getString("last_error")))
                .list();
    }

    @Override
    public void save(OutboxMessage message) {
        jdbc.sql("""
                UPDATE edu_outbox_message
                SET status = :status,
                    retry_count = :retryCount,
                    next_retry_at = :nextRetryAt,
                    last_error = :lastError,
                    published_at = CASE WHEN :status = 'PUBLISHED' THEN CURRENT_TIMESTAMP(3) ELSE published_at END
                WHERE id = :id
                """)
                .param("status", message.getStatus().name())
                .param("retryCount", message.getRetryCount())
                .param("nextRetryAt", message.getNextRetryAt())
                .param("lastError", message.getLastError())
                .param("id", message.getId().toString())
                .update();
    }

    private String writeJson(CoursePurchasedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化本地消息失败", ex);
        }
    }

    private CoursePurchasedEvent readEvent(String payload) {
        try {
            return objectMapper.readValue(payload, CoursePurchasedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("反序列化本地消息失败", ex);
        }
    }
}
