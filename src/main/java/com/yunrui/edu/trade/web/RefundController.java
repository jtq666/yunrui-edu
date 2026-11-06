package com.yunrui.edu.trade.web;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.common.id.IdGenerator;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/refunds")
public class RefundController {
    private final JdbcClient jdbc; private final UserAccessGuard guard; private final IdGenerator ids;
    private final PurchaseOrderRepository orders;
    public RefundController(JdbcClient jdbc,UserAccessGuard guard,IdGenerator ids,PurchaseOrderRepository orders){
        this.jdbc=jdbc;this.guard=guard;this.ids=ids;this.orders=orders;
    }

    @GetMapping
    public List<RefundView> list(@PathVariable long userId){guard.require(userId);return jdbc.sql("SELECT id,order_id,reason,status,decision_reason,created_at,handled_at FROM edu_refund_request WHERE user_id=:userId ORDER BY created_at DESC").param("userId",userId).query((rs,n)->map(rs)).list();}

    @PostMapping
    @Transactional
    public RefundView apply(@PathVariable long userId,@Valid @RequestBody ApplyRefund request){
        guard.require(userId); PurchaseOrder order=orders.findByIdForUpdate(request.orderId()).orElseThrow(()->new IllegalArgumentException("订单不存在"));
        if(order.getUserId()!=userId) throw new IllegalStateException("不能申请其他用户的订单退款");
        if(order.getStatus()!=com.yunrui.edu.trade.domain.OrderStatus.PAID) throw new IllegalStateException("只有已支付订单可以申请退款");
        long id=ids.nextId();
        try{jdbc.sql("INSERT INTO edu_refund_request(id,order_id,user_id,reason,status) VALUES(:id,:orderId,:userId,:reason,'PENDING')").param("id",id).param("orderId",request.orderId()).param("userId",userId).param("reason",request.reason().trim()).update();}
        catch(org.springframework.dao.DuplicateKeyException e){throw new IllegalStateException("该订单已经申请过退款");}
        return find(userId,id);
    }

    private RefundView find(long userId,long id){return jdbc.sql("SELECT id,order_id,reason,status,decision_reason,created_at,handled_at FROM edu_refund_request WHERE id=:id AND user_id=:userId").param("id",id).param("userId",userId).query((rs,n)->map(rs)).optional().orElseThrow(()->new IllegalArgumentException("退款申请不存在"));}
    private RefundView map(java.sql.ResultSet rs)throws java.sql.SQLException{return new RefundView(rs.getLong("id"),rs.getLong("order_id"),rs.getString("reason"),rs.getString("status"),rs.getString("decision_reason"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("handled_at")==null?null:rs.getTimestamp("handled_at").toInstant());}
    public record ApplyRefund(long orderId,@NotBlank @Size(max=300) String reason){}
    public record RefundView(long id,long orderId,String reason,String status,String decisionReason,Instant createdAt,Instant handledAt){}
}
