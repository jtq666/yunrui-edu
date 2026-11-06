package com.yunrui.edu.learning.progress;

import java.util.List;
import java.util.Optional;

public interface ProgressBuffer {
    void save(ProgressSnapshot snapshot);

    Optional<ProgressSnapshot> find(long userId, long courseId, long sectionId);

    List<ProgressSnapshot> findByUserAndCourse(long userId, long courseId);

    boolean acknowledgeIfUnchanged(ProgressSnapshot snapshot);
}
