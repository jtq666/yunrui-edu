package com.yunrui.edu.course;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.common.id.IdGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
public class CourseReviewController {
    private final JdbcClient jdbc; private final UserAccessGuard guard; private final IdGenerator ids;
    public CourseReviewController(JdbcClient jdbc,UserAccessGuard guard,IdGenerator ids){this.jdbc=jdbc;this.guard=guard;this.ids=ids;}

    @GetMapping
    public ReviewSummary list(@PathVariable long courseId){
        List<Review> reviews=jdbc.sql("""
                SELECT r.id,r.user_id,u.nickname,r.score,r.content,r.created_at
                FROM edu_course_review r JOIN edu_user_account u ON u.id=r.user_id
                WHERE r.course_id=:courseId ORDER BY r.created_at DESC
                """).param("courseId",courseId).query((rs,n)->new Review(rs.getLong("id"),rs.getLong("user_id"),rs.getString("nickname"),rs.getInt("score"),rs.getString("content"),rs.getTimestamp("created_at").toInstant())).list();
        double average=reviews.stream().mapToInt(Review::score).average().orElse(0);
        return new ReviewSummary(Math.round(average*10)/10.0,reviews.size(),reviews);
    }

    @PostMapping
    public ReviewSummary create(@PathVariable long courseId,@Valid @RequestBody ReviewRequest request){
        guard.require(request.userId());
        long owned=jdbc.sql("SELECT COUNT(*) FROM edu_course_entitlement WHERE user_id=:userId AND course_id=:courseId")
                .param("userId",request.userId()).param("courseId",courseId).query(Long.class).single();
        if(owned==0) throw new IllegalStateException("购买并获得课程后才能评价");
        try{jdbc.sql("INSERT INTO edu_course_review(id,user_id,course_id,score,content) VALUES(:id,:userId,:courseId,:score,:content)")
                .param("id",ids.nextId()).param("userId",request.userId()).param("courseId",courseId).param("score",request.score()).param("content",request.content().trim()).update();}
        catch(org.springframework.dao.DuplicateKeyException e){throw new IllegalStateException("每门课程只能评价一次");}
        return list(courseId);
    }
    public record ReviewRequest(long userId,@Min(1) @Max(5) int score,@NotBlank @Size(max=500) String content){}
    public record Review(long id,long userId,String nickname,int score,String content,Instant createdAt){}
    public record ReviewSummary(double average,int total,List<Review> records){}
}
