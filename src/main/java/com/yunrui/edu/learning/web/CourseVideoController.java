package com.yunrui.edu.learning.web;

import com.yunrui.edu.auth.UserAccessGuard;
import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class CourseVideoController {
    private final JdbcClient jdbc;
    private final UserAccessGuard guard;
    private final CourseEntitlementRepository entitlements;
    private final StringRedisTemplate redis;
    private final Path mediaRoot;

    public CourseVideoController(JdbcClient jdbc, UserAccessGuard guard,
                                 CourseEntitlementRepository entitlements,
                                 StringRedisTemplate redis,
                                 @Value("${app.media.root}") String mediaRoot) {
        this.jdbc = jdbc; this.guard = guard; this.entitlements = entitlements; this.redis = redis;
        this.mediaRoot = Path.of(mediaRoot).toAbsolutePath().normalize();
    }

    @PostMapping("/sections/{sectionId}/ticket")
    public Ticket ticket(@PathVariable long sectionId, @RequestBody TicketRequest request) {
        guard.require(request.userId());
        Video video = findVideo(sectionId);
        if (!entitlements.existsByUserAndCourse(request.userId(), video.courseId())) {
            throw new IllegalStateException("尚未购买该课程，不能播放视频");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("video:ticket:" + ticket, request.userId() + ":" + sectionId, Duration.ofMinutes(5));
        return new Ticket(ticket, 300);
    }

    @GetMapping("/sections/{sectionId}")
    public ResponseEntity<byte[]> play(@PathVariable long sectionId,
                                  @RequestParam String ticket,
                                  @RequestHeader HttpHeaders headers) throws IOException {
        String value = redis.opsForValue().get("video:ticket:" + ticket);
        if (value == null || !value.endsWith(":" + sectionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long userId = Long.parseLong(value.substring(0, value.indexOf(':')));
        Video video = findVideo(sectionId);
        if (!entitlements.existsByUserAndCourse(userId, video.courseId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (video.fileName() == null || video.fileName().isBlank()) {
            throw new IllegalArgumentException("该章节暂未配置视频");
        }
        return stream(video.fileName(), headers);
    }

    /**
     * 第一章节允许未购买用户试听，其他章节仍必须购买后通过短期 ticket 播放。
     * 试听也走同一套 Range 分段读取逻辑，避免把整个视频一次性加载到内存。
     */
    @GetMapping("/sections/{sectionId}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable long sectionId,
                                          @RequestHeader HttpHeaders headers) throws IOException {
        int sortNo = jdbc.sql("""
                SELECT s.sort_no
                FROM edu_course_section s
                JOIN edu_course c ON c.id=s.course_id
                WHERE s.id=:id AND c.status='PUBLISHED'
                """)
                .param("id", sectionId)
                .query(Integer.class)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("课程章节不存在"));
        if (sortNo != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return stream(findVideo(sectionId).fileName(), headers);
    }

    private ResponseEntity<byte[]> stream(String fileName, HttpHeaders headers) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("该章节暂未配置视频");
        }
        Path file = mediaRoot.resolve(fileName).normalize();
        if (!file.startsWith(mediaRoot)) throw new IllegalStateException("视频路径不合法");
        if (!java.nio.file.Files.exists(file)) throw new IllegalStateException("视频文件不存在");

        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            byte[] all = java.nio.file.Files.readAllBytes(file);
            return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4"))
                    .contentLength(all.length).header(HttpHeaders.ACCEPT_RANGES,"bytes").body(all);
        }
        long fileLength = java.nio.file.Files.size(file);
        long start = ranges.get(0).getRangeStart(fileLength);
        long requestedEnd = ranges.get(0).getRangeEnd(fileLength);
        long end = Math.min(requestedEnd, Math.min(fileLength - 1, start + 1024 * 1024 - 1));
        byte[] chunk = new byte[(int) (end - start + 1)];
        try (RandomAccessFile input = new RandomAccessFile(file.toFile(), "r")) {
            input.seek(start); input.readFully(chunk);
        }
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(MediaType.valueOf("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES,"bytes")
                .header(HttpHeaders.CONTENT_RANGE,"bytes " + start + "-" + end + "/" + fileLength)
                .contentLength(chunk.length).body(chunk);
    }
    private Video findVideo(long sectionId) {
        return jdbc.sql("SELECT course_id, video_file FROM edu_course_section WHERE id=:id")
                .param("id", sectionId)
                .query((rs,n)->new Video(rs.getLong("course_id"),rs.getString("video_file")))
                .optional().orElseThrow(()->new IllegalArgumentException("课程章节不存在"));
    }
    public record TicketRequest(long userId){}
    public record Ticket(String ticket,int expiresInSeconds){}
    private record Video(long courseId,String fileName){}
}
