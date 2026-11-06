package com.yunrui.edu.course;

import com.yunrui.edu.common.id.IdGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseAdminController {
    private final JdbcClient jdbc; private final IdGenerator ids; private final Path mediaRoot;
    public CourseAdminController(JdbcClient jdbc,IdGenerator ids,@Value("${app.media.root}") String mediaRoot){this.jdbc=jdbc;this.ids=ids;this.mediaRoot=Path.of(mediaRoot).toAbsolutePath().normalize();}

    @GetMapping
    public List<AdminCourse> list(){requireAdmin();return jdbc.sql("SELECT id,title,category,teacher_name,price,status,description FROM edu_course ORDER BY created_at DESC").query((rs,n)->new AdminCourse(rs.getLong("id"),rs.getString("title"),rs.getString("category"),rs.getString("teacher_name"),rs.getBigDecimal("price"),rs.getString("status"),rs.getString("description"))).list();}

    @PostMapping
    @Transactional
    public AdminCourse create(@Valid @RequestBody SaveCourse request){requireAdmin();long id=ids.nextId();jdbc.sql("INSERT INTO edu_course(id,title,category,teacher_name,price,status,description) VALUES(:id,:title,:category,:teacher,:price,'DRAFT',:description)").param("id",id).param("title",request.title()).param("category",request.category()).param("teacher",request.teacherName()).param("price",request.price()).param("description",request.description()).update();return find(id);}

    @PutMapping("/{courseId}")
    @Transactional
    public AdminCourse update(@PathVariable long courseId,@Valid @RequestBody SaveCourse request){requireAdmin();int n=jdbc.sql("UPDATE edu_course SET title=:title,category=:category,teacher_name=:teacher,price=:price,description=:description WHERE id=:id").param("title",request.title()).param("category",request.category()).param("teacher",request.teacherName()).param("price",request.price()).param("description",request.description()).param("id",courseId).update();if(n==0)throw new IllegalArgumentException("课程不存在");return find(courseId);}

    @PostMapping("/{courseId}/status")
    public AdminCourse status(@PathVariable long courseId,@RequestParam String value){requireAdmin();if(!List.of("DRAFT","PUBLISHED","OFFLINE").contains(value))throw new IllegalArgumentException("课程状态不合法");jdbc.sql("UPDATE edu_course SET status=:status WHERE id=:id").param("status",value).param("id",courseId).update();return find(courseId);}

    @GetMapping("/{courseId}/sections")
    public List<AdminSection> sections(@PathVariable long courseId){requireAdmin();find(courseId);return jdbc.sql("SELECT id,title,sort_no,duration_seconds,video_file FROM edu_course_section WHERE course_id=:courseId ORDER BY sort_no").param("courseId",courseId).query((rs,n)->new AdminSection(rs.getLong("id"),rs.getString("title"),rs.getInt("sort_no"),rs.getInt("duration_seconds"),rs.getString("video_file")!=null)).list();}

    @PostMapping("/{courseId}/sections")
    @Transactional
    public AdminSection createSection(@PathVariable long courseId,@Valid @RequestBody SaveSection request){requireAdmin();find(courseId);long id=ids.nextId();jdbc.sql("INSERT INTO edu_course_section(id,course_id,title,sort_no,duration_seconds) VALUES(:id,:courseId,:title,:sortNo,:duration)").param("id",id).param("courseId",courseId).param("title",request.title()).param("sortNo",request.sortNo()).param("duration",request.durationSeconds()).update();return section(id);}

    @PutMapping("/sections/{sectionId}")
    public AdminSection updateSection(@PathVariable long sectionId,@Valid @RequestBody SaveSection request){requireAdmin();int n=jdbc.sql("UPDATE edu_course_section SET title=:title,sort_no=:sortNo,duration_seconds=:duration WHERE id=:id").param("title",request.title()).param("sortNo",request.sortNo()).param("duration",request.durationSeconds()).param("id",sectionId).update();if(n==0)throw new IllegalArgumentException("章节不存在");return section(sectionId);}

    @PostMapping(value="/sections/{sectionId}/video",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminSection uploadVideo(@PathVariable long sectionId,@RequestPart("file") MultipartFile file) throws IOException {requireAdmin();section(sectionId);if(file.isEmpty())throw new IllegalArgumentException("视频文件不能为空");if(file.getSize()>200L*1024*1024)throw new IllegalArgumentException("视频不能超过200MB");String original=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase();String type=file.getContentType()==null?"":file.getContentType();if(!original.endsWith(".mp4")&&!"video/mp4".equalsIgnoreCase(type))throw new IllegalArgumentException("目前只支持MP4视频");Files.createDirectories(mediaRoot);String name="course-"+sectionId+"-"+UUID.randomUUID().toString().replace("-","")+".mp4";Path target=mediaRoot.resolve(name).normalize();if(!target.startsWith(mediaRoot))throw new IllegalStateException("视频路径不合法");file.transferTo(target);String old=jdbc.sql("SELECT video_file FROM edu_course_section WHERE id=:id").param("id",sectionId).query(String.class).optional().orElse(null);jdbc.sql("UPDATE edu_course_section SET video_file=:file WHERE id=:id").param("file",name).param("id",sectionId).update();if(old!=null&&old.startsWith("course-")&&!old.equals(name))Files.deleteIfExists(mediaRoot.resolve(old));return section(sectionId);}

    @DeleteMapping("/sections/{sectionId}")
    @Transactional
    public void deleteSection(@PathVariable long sectionId) throws IOException {requireAdmin();AdminSection item=section(sectionId);String file=jdbc.sql("SELECT video_file FROM edu_course_section WHERE id=:id").param("id",sectionId).query(String.class).optional().orElse(null);jdbc.sql("DELETE FROM edu_course_section WHERE id=:id").param("id",sectionId).update();if(file!=null&&file.startsWith("course-"))Files.deleteIfExists(mediaRoot.resolve(file));}

    private AdminCourse find(long id){return jdbc.sql("SELECT id,title,category,teacher_name,price,status,description FROM edu_course WHERE id=:id").param("id",id).query((rs,n)->new AdminCourse(rs.getLong("id"),rs.getString("title"),rs.getString("category"),rs.getString("teacher_name"),rs.getBigDecimal("price"),rs.getString("status"),rs.getString("description"))).optional().orElseThrow(()->new IllegalArgumentException("课程不存在"));}
    private AdminSection section(long id){return jdbc.sql("SELECT id,title,sort_no,duration_seconds,video_file FROM edu_course_section WHERE id=:id").param("id",id).query((rs,n)->new AdminSection(rs.getLong("id"),rs.getString("title"),rs.getInt("sort_no"),rs.getInt("duration_seconds"),rs.getString("video_file")!=null)).optional().orElseThrow(()->new IllegalArgumentException("章节不存在"));}
    private void requireAdmin(){String uid=SecurityContextHolder.getContext().getAuthentication().getName();long n=jdbc.sql("SELECT COUNT(*) FROM edu_user_account WHERE id=:id AND role='ADMIN'").param("id",Long.parseLong(uid)).query(Long.class).single();if(n==0)throw new IllegalStateException("需要管理员权限");}
    public record SaveCourse(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=64) String category,@NotBlank @Size(max=64) String teacherName,@DecimalMin("0.00") BigDecimal price,@Size(max=2000) String description){}
    public record AdminCourse(long id,String title,String category,String teacherName,BigDecimal price,String status,String description){}
    public record SaveSection(@NotBlank @Size(max=200) String title,@jakarta.validation.constraints.Min(1) int sortNo,@jakarta.validation.constraints.Positive int durationSeconds){}
    public record AdminSection(long id,String title,int sortNo,int durationSeconds,boolean videoAvailable){}
}
