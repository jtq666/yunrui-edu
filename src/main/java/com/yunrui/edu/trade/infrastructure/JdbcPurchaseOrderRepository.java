package com.yunrui.edu.trade.infrastructure;

import com.yunrui.edu.trade.domain.OrderStatus;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class JdbcPurchaseOrderRepository implements PurchaseOrderRepository {
    private final JdbcClient jdbc;

    public JdbcPurchaseOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(PurchaseOrder order) {
        jdbc.sql("""
                INSERT INTO edu_purchase_order(
                    id, user_id, course_id, status, original_amount, discount_amount, payable_amount)
                VALUES (:id, :userId, :courseId, :status, :original, :discount, :payable)
                """)
                .param("id", order.getId())
                .param("userId", order.getUserId())
                .param("courseId", order.getCourseId())
                .param("status", order.getStatus().name())
                .param("original", order.getOriginalAmount())
                .param("discount", order.getDiscountAmount())
                .param("payable", order.getPayableAmount())
                .update();
    }

    @Override
    public Optional<PurchaseOrder> findByIdForUpdate(long orderId) {
        return jdbc.sql("""
                SELECT id, user_id, course_id, status, original_amount, discount_amount,
                       payable_amount, payment_transaction_id, paid_at
                FROM edu_purchase_order WHERE id = :id FOR UPDATE
                """)
                .param("id", orderId)
                .query((rs, rowNum) -> mapOrder(rs))
                .optional();
    }

    @Override
    public Optional<PurchaseOrder> findById(long orderId) {
        return jdbc.sql("""
                SELECT id, user_id, course_id, status, original_amount, discount_amount,
                       payable_amount, payment_transaction_id, paid_at
                FROM edu_purchase_order WHERE id = :id
                """)
                .param("id", orderId)
                .query((rs, rowNum) -> mapOrder(rs))
                .optional();
    }

    @Override
    public List<PurchaseOrder> findByUserId(long userId) {
        return jdbc.sql("""
                SELECT id, user_id, course_id, status, original_amount, discount_amount,
                       payable_amount, payment_transaction_id, paid_at
                FROM edu_purchase_order
                WHERE user_id = :userId
                ORDER BY created_at DESC
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> mapOrder(rs))
                .list();
    }

    @Override
    public void save(PurchaseOrder order) {
        jdbc.sql("""
                UPDATE edu_purchase_order
                SET status = :status,
                    payment_transaction_id = :transactionId,
                    paid_at = :paidAt
                WHERE id = :id
                """)
                .param("status", order.getStatus().name())
                .param("transactionId", order.getPaymentTransactionId())
                .param("paidAt", order.getPaidAt())
                .param("id", order.getId())
                .update();
    }

    private PurchaseOrder mapOrder(ResultSet rs) throws SQLException {
        Timestamp paidAt = rs.getTimestamp("paid_at");
        return PurchaseOrder.restore(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("course_id"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("original_amount"), rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("payable_amount"), rs.getString("payment_transaction_id"),
                paidAt == null ? null : paidAt.toInstant());
    }
}
