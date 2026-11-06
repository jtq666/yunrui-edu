package com.yunrui.edu.student;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.course.Course;
import com.yunrui.edu.course.CourseCatalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}")
public class StudentCenterController {
    private final JdbcClient jdbc;
    private final UserAccessGuard guard;
    private final CourseCatalog courses;

    public StudentCenterController(JdbcClient jdbc, UserAccessGuard guard, CourseCatalog courses) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.courses = courses;
    }

    @GetMapping("/profile")
    public Profile profile(@PathVariable long userId) {
        guard.require(userId);
        return jdbc.sql("SELECT id, username, nickname, email, bio, role FROM edu_user_account WHERE id=:id")
                .param("id", userId).query((rs, n) -> new Profile(rs.getLong("id"), rs.getString("username"),
                        rs.getString("nickname"), rs.getString("email"), rs.getString("bio"), rs.getString("role")))
                .optional().orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @PutMapping("/profile")
    @Transactional
    public Profile updateProfile(@PathVariable long userId, @Valid @RequestBody UpdateProfile request) {
        guard.require(userId);
        jdbc.sql("UPDATE edu_user_account SET nickname=:nickname,email=:email,bio=:bio WHERE id=:id")
                .param("nickname", request.nickname().trim()).param("email", request.email())
                .param("bio", request.bio()).param("id", userId).update();
        return profile(userId);
    }

    @GetMapping("/cart")
    public List<Course> cart(@PathVariable long userId) {
        guard.require(userId);
        return relationCourses("edu_cart_item", userId);
    }

    @PostMapping("/cart/{courseId}")
    @Transactional
    public void addCart(@PathVariable long userId, @PathVariable long courseId) {
        guard.require(userId); requireCourse(courseId);
        jdbc.sql("INSERT IGNORE INTO edu_cart_item(user_id,course_id) VALUES(:userId,:courseId)")
                .param("userId", userId).param("courseId", courseId).update();
    }

    @DeleteMapping("/cart/{courseId}")
    public void removeCart(@PathVariable long userId, @PathVariable long courseId) {
        guard.require(userId);
        jdbc.sql("DELETE FROM edu_cart_item WHERE user_id=:userId AND course_id=:courseId")
                .param("userId", userId).param("courseId", courseId).update();
    }

    @GetMapping("/favorites")
    public List<Course> favorites(@PathVariable long userId) {
        guard.require(userId);
        return relationCourses("edu_course_favorite", userId);
    }

    @PostMapping("/favorites/{courseId}")
    @Transactional
    public void addFavorite(@PathVariable long userId, @PathVariable long courseId) {
        guard.require(userId); requireCourse(courseId);
        jdbc.sql("INSERT IGNORE INTO edu_course_favorite(user_id,course_id) VALUES(:userId,:courseId)")
                .param("userId", userId).param("courseId", courseId).update();
    }

    @DeleteMapping("/favorites/{courseId}")
    public void removeFavorite(@PathVariable long userId, @PathVariable long courseId) {
        guard.require(userId);
        jdbc.sql("DELETE FROM edu_course_favorite WHERE user_id=:userId AND course_id=:courseId")
                .param("userId", userId).param("courseId", courseId).update();
    }

    private List<Course> relationCourses(String table, long userId) {
        List<Long> ids = jdbc.sql("SELECT course_id FROM " + table + " WHERE user_id=:userId ORDER BY created_at DESC")
                .param("userId", userId).query(Long.class).list();
        return ids.stream().map(id -> courses.findPublished(id).orElse(null)).filter(java.util.Objects::nonNull).toList();
    }

    private void requireCourse(long courseId) {
        courses.findPublished(courseId).orElseThrow(() -> new IllegalArgumentException("课程不存在或尚未发布"));
    }

    public record Profile(long id, String username, String nickname, String email, String bio, String role) {}
    public record UpdateProfile(@NotBlank @Size(max=64) String nickname,
                                @Email @Size(max=120) String email,
                                @Size(max=300) String bio) {}
}
