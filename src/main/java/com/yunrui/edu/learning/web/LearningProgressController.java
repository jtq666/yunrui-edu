package com.yunrui.edu.learning.web;

import com.yunrui.edu.learning.progress.LearningProgressService;
import com.yunrui.edu.learning.progress.ProgressSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.yunrui.edu.auth.UserAccessGuard;

@RestController
@RequestMapping("/api/learning-progress")
public class LearningProgressController {
    private final LearningProgressService service;
    private final UserAccessGuard accessGuard;

    public LearningProgressController(LearningProgressService service, UserAccessGuard accessGuard) {
        this.service = service;
        this.accessGuard = accessGuard;
    }

    @PostMapping
    public ReportResult report(@Valid @RequestBody ReportRequest request) {
        accessGuard.require(request.userId());
        boolean accepted = service.report(
                request.userId(), request.courseId(), request.sectionId(),
                request.positionSeconds(), request.durationSeconds());
        return new ReportResult(accepted);
    }

    @GetMapping("/users/{userId}/courses/{courseId}")
    public List<ProgressSnapshot> query(@PathVariable long userId, @PathVariable long courseId) {
        accessGuard.require(userId);
        return service.query(userId, courseId);
    }

    public record ReportRequest(@Positive long userId,
                                @Positive long courseId,
                                @Positive long sectionId,
                                @Min(0) int positionSeconds,
                                @Positive int durationSeconds) {
    }

    public record ReportResult(boolean accepted) {
    }
}
