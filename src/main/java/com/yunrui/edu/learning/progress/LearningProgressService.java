package com.yunrui.edu.learning.progress;

import com.yunrui.edu.learning.domain.CourseEntitlementRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningProgressService {
    private final ProgressBuffer buffer;
    private final LearningProgressRepository repository;
    private final Clock clock;
    private final CourseEntitlementRepository entitlementRepository;
    private final LearningProgressDelayTaskHandler delayTaskHandler;

    public LearningProgressService(ProgressBuffer buffer,
                                   LearningProgressRepository repository,
                                   Clock clock,
                                   CourseEntitlementRepository entitlementRepository,
                                   LearningProgressDelayTaskHandler delayTaskHandler) {
        this.buffer = buffer;
        this.repository = repository;
        this.clock = clock;
        this.entitlementRepository = entitlementRepository;
        this.delayTaskHandler = delayTaskHandler;
    }

    public boolean report(long userId,
                          long courseId,
                          long sectionId,
                          int positionSeconds,
                          int durationSeconds) {
        if (!entitlementRepository.existsByUserAndCourse(userId, courseId)) {
            throw new IllegalStateException("尚未购买该课程，不能上报学习进度");
        }
        if (!repository.sectionBelongsToCourse(sectionId, courseId)) {
            throw new IllegalArgumentException("章节不属于当前课程，不能上报学习进度");
        }
        ProgressSnapshot snapshot = new ProgressSnapshot(
                userId, courseId, sectionId, positionSeconds,
                durationSeconds, clock.instant());
        buffer.save(snapshot);
        if (snapshot.completed()) {
            repository.upsertBatch(List.of(snapshot));
            buffer.acknowledgeIfUnchanged(snapshot);
        } else {
            delayTaskHandler.schedule(snapshot);
        }
        return true;
    }

    public List<ProgressSnapshot> query(long userId, long courseId) {
        Map<Long, ProgressSnapshot> merged = new LinkedHashMap<>();
        repository.findByUserAndCourse(userId, courseId)
                .forEach(snapshot -> merged.put(snapshot.sectionId(), snapshot));
        buffer.findByUserAndCourse(userId, courseId).forEach(snapshot ->
                merged.merge(snapshot.sectionId(), snapshot,
                        (stored, cached) -> cached.reportedAt().isAfter(stored.reportedAt())
                                ? cached : stored));
        return new ArrayList<>(merged.values());
    }
}
