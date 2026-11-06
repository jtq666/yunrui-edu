package com.yunrui.edu.trade.web;

import com.yunrui.edu.common.id.IdGenerator;
import com.yunrui.edu.trade.domain.PurchaseOrder;
import com.yunrui.edu.trade.domain.PurchaseOrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/refunds")
public class RefundAdminController {
 private final JdbcClient jdbc;private final PurchaseOrderRepository orders;private final IdGenerator ids;
 public RefundAdminController(JdbcClient jdbc,PurchaseOrderRepository orders,IdGenerator ids){this.jdbc=jdbc;this.orders=orders;this.ids=ids;}
 @GetMapping public List<RefundItem> list(@RequestParam(defaultValue="PENDING") String status){requireAdmin();String sql="ALL".equals(status)?"SELECT id,order_id,user_id,reason,status,decision_reason,created_at,handled_at FROM edu_refund_request ORDER BY created_at DESC":"SELECT id,order_id,user_id,reason,status,decision_reason,created_at,handled_at FROM edu_refund_request WHERE status=:status ORDER BY created_at";var spec=jdbc.sql(sql);if(!"ALL".equals(status))spec=spec.param("status",status);return spec.query((rs,n)->map(rs)).list();}
 @PostMapping("/{id}/approve") @Transactional public RefundItem approve(@PathVariable long id,@Valid @RequestBody Decision r){requireAdmin();RefundItem item=findForUpdate(id);if(!"PENDING".equals(item.status()))throw new IllegalStateException("退款申请已经处理");PurchaseOrder order=orders.findByIdForUpdate(item.orderId()).orElseThrow(()->new IllegalArgumentException("订单不存在"));order.refund();orders.save(order);jdbc.sql("DELETE FROM edu_course_entitlement WHERE order_id=:orderId AND user_id=:userId").param("orderId",order.getId()).param("userId",item.userId()).update();finish(id,"APPROVED",r.reason());notify(item.userId(),"退款申请已通过","订单"+item.orderId()+"退款审核已通过，课程权限已经收回");return find(id);}
 @PostMapping("/{id}/reject") @Transactional public RefundItem reject(@PathVariable long id,@Valid @RequestBody Decision r){requireAdmin();RefundItem item=findForUpdate(id);if(!"PENDING".equals(item.status()))throw new IllegalStateException("退款申请已经处理");finish(id,"REJECTED",r.reason());notify(item.userId(),"退款申请未通过","订单"+item.orderId()+"退款未通过："+r.reason());return find(id);}
 private void finish(long id,String status,String reason){jdbc.sql("UPDATE edu_refund_request SET status=:status,decision_reason=:reason,handled_at=CURRENT_TIMESTAMP(3) WHERE id=:id AND status='PENDING'").param("status",status).param("reason",reason.trim()).param("id",id).update();}
 private void notify(long userId,String title,String content){jdbc.sql("INSERT INTO edu_notification(id,user_id,title,content) VALUES(:id,:userId,:title,:content)").param("id",ids.nextId()).param("userId",userId).param("title",title).param("content",content).update();}
 private RefundItem findForUpdate(long id){return jdbc.sql("SELECT id,order_id,user_id,reason,status,decision_reason,created_at,handled_at FROM edu_refund_request WHERE id=:id FOR UPDATE").param("id",id).query((rs,n)->map(rs)).optional().orElseThrow(()->new IllegalArgumentException("退款申请不存在"));}
 private RefundItem find(long id){return jdbc.sql("SELECT id,order_id,user_id,reason,status,decision_reason,created_at,handled_at FROM edu_refund_request WHERE id=:id").param("id",id).query((rs,n)->map(rs)).single();}
 private RefundItem map(java.sql.ResultSet rs)throws java.sql.SQLException{return new RefundItem(rs.getLong("id"),rs.getLong("order_id"),rs.getLong("user_id"),rs.getString("reason"),rs.getString("status"),rs.getString("decision_reason"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("handled_at")==null?null:rs.getTimestamp("handled_at").toInstant());}
 private void requireAdmin(){long id=Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());long n=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE id=:id AND role='ADMIN'").param("id",id).query(Long.class).single();if(n==0)throw new IllegalStateException("需要管理员权限");}
 public record Decision(@NotBlank @Size(max=300) String reason){}
 public record RefundItem(long id,long orderId,long userId,String reason,String status,String decisionReason,Instant createdAt,Instant handledAt){}
}
