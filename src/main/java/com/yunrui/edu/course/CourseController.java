package com.yunrui.edu.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseCatalog catalog;

    public CourseController(CourseCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<Course> search(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "") String category,
                               @RequestParam(defaultValue = "20") int limit) {
        return catalog.searchPublished(keyword, category, limit);
    }

    @GetMapping("/{courseId}")
    public Course detail(@PathVariable long courseId) {
        return catalog.findPublished(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在或尚未发布"));
    }

    @GetMapping("/page")
    public CourseCatalog.PageResult<Course> page(@RequestParam(defaultValue = "") String keyword,
                                                  @RequestParam(defaultValue = "") String category,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "8") int pageSize) {
        return catalog.pagePublished(keyword, category, page, pageSize);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return catalog.categories();
    }
}
