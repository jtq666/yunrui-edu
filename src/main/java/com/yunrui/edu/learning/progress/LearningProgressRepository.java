package com.yunrui.edu.learning.progress;

import java.util.List;

public interface LearningProgressRepository {
    void upsertBatch(List<ProgressSnapshot> snapshots);

    List<ProgressSnapshot> findByUserAndCourse(long userId, long courseId);

    boolean sectionBelongsToCourse(long sectionId, long courseId);
}
