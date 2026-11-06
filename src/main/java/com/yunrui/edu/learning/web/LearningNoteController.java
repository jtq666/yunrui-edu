package com.yunrui.edu.learning.web;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.common.id.IdGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/notes")
public class LearningNoteController {
    private final JdbcClient jdbc;
    private final UserAccessGuard guard;
    private final IdGenerator ids;

    public LearningNoteController(JdbcClient jdbc, UserAccessGuard guard, IdGenerator ids) {
        this.jdbc = jdbc; this.guard = guard; this.ids = ids;
    }

    @GetMapping
    public List<Note> list(@PathVariable long userId, @RequestParam long courseId) {
        guard.require(userId); requireEntitlement(userId, courseId);
        return jdbc.sql("""
                SELECT id,course_id,section_id,content,video_second,created_at,updated_at
                FROM edu_learning_note WHERE user_id=:userId AND course_id=:courseId ORDER BY updated_at DESC
                """).param("userId", userId).param("courseId", courseId)
                .query((rs,n)->new Note(rs.getLong("id"),rs.getLong("course_id"),rs.getLong("section_id"),
                        rs.getString("content"),rs.getInt("video_second"),rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant())).list();
    }

    @PostMapping
    public Note create(@PathVariable long userId, @Valid @RequestBody SaveNote request) {
        guard.require(userId); requireEntitlement(userId, request.courseId()); requireSection(request.sectionId(), request.courseId());
        long id=ids.nextId();
        jdbc.sql("INSERT INTO edu_learning_note(id,user_id,course_id,section_id,content,video_second) VALUES(:id,:userId,:courseId,:sectionId,:content,:second)")
                .param("id",id).param("userId",userId).param("courseId",request.courseId())
                .param("sectionId",request.sectionId()).param("content",request.content().trim())
                .param("second",Math.max(request.videoSecond(),0)).update();
        return findOwned(userId,id);
    }

    @PutMapping("/{noteId}")
    public Note update(@PathVariable long userId,@PathVariable long noteId,@Valid @RequestBody SaveNote request) {
        guard.require(userId); requireEntitlement(userId,request.courseId()); requireSection(request.sectionId(), request.courseId());
        int changed=jdbc.sql("UPDATE edu_learning_note SET content=:content,video_second=:second WHERE id=:id AND user_id=:userId")
                .param("content",request.content().trim()).param("second",Math.max(request.videoSecond(),0))
                .param("id",noteId).param("userId",userId).update();
        if(changed==0) throw new IllegalArgumentException("笔记不存在");
        return findOwned(userId,noteId);
    }

    @DeleteMapping("/{noteId}")
    public void delete(@PathVariable long userId,@PathVariable long noteId) {
        guard.require(userId);
        jdbc.sql("DELETE FROM edu_learning_note WHERE id=:id AND user_id=:userId")
                .param("id",noteId).param("userId",userId).update();
    }

    private Note findOwned(long userId,long id){
        return jdbc.sql("SELECT id,course_id,section_id,content,video_second,created_at,updated_at FROM edu_learning_note WHERE id=:id AND user_id=:userId")
                .param("id",id).param("userId",userId).query((rs,n)->new Note(rs.getLong("id"),rs.getLong("course_id"),rs.getLong("section_id"),rs.getString("content"),rs.getInt("video_second"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant()))
                .optional().orElseThrow(()->new IllegalArgumentException("笔记不存在"));
    }

    private void requireEntitlement(long userId,long courseId){
        long count=jdbc.sql("SELECT COUNT(*) FROM edu_course_entitlement WHERE user_id=:userId AND course_id=:courseId")
                .param("userId",userId).param("courseId",courseId).query(Long.class).single();
        if(count==0) throw new IllegalStateException("购买课程后才能记录学习笔记");
    }

    private void requireSection(long sectionId,long courseId){
        long count=jdbc.sql("SELECT COUNT(*) FROM edu_course_section WHERE id=:sectionId AND course_id=:courseId")
                .param("sectionId",sectionId).param("courseId",courseId).query(Long.class).single();
        if(count==0) throw new IllegalArgumentException("章节不属于当前课程");
    }

    public record SaveNote(@Positive long courseId,@Positive long sectionId,@NotBlank @Size(max=1000) String content,int videoSecond){}
    public record Note(long id,long courseId,long sectionId,String content,int videoSecond,Instant createdAt,Instant updatedAt){}
}
