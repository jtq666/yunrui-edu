package com.yunrui.edu.learning.progress;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Component
public class LearningProgressDelayTaskHandler implements InitializingBean, DisposableBean {
    private final ProgressBuffer buffer;
    private final LearningProgressRepository repository;
    private final Duration delay;
    private final DelayQueue<ProgressDelayTask> queue = new DelayQueue<>();
    private volatile boolean running;
    private Thread worker;

    public LearningProgressDelayTaskHandler(
            ProgressBuffer buffer,
            LearningProgressRepository repository,
            @Value("${app.learning-progress.persist-delay-ms:20000}") long delayMillis) {
        this.buffer = buffer;
        this.repository = repository;
        this.delay = Duration.ofMillis(delayMillis);
    }

    public void schedule(ProgressSnapshot snapshot) {
        queue.offer(new ProgressDelayTask(snapshot, delay));
    }

    @Override
    public void afterPropertiesSet() {
        running = true;
        worker = new Thread(this::consume, "learning-progress-delay-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void consume() {
        while (running) {
            try {
                persistIfUnchanged(queue.take().snapshot());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ignored) {
                // 缓存保留七天。临时写库失败时不删除，后续上报会再次创建延迟任务。
            }
        }
    }

    boolean persistIfUnchanged(ProgressSnapshot taskSnapshot) {
        ProgressSnapshot latest = buffer.find(
                        taskSnapshot.userId(), taskSnapshot.courseId(), taskSnapshot.sectionId())
                .orElse(null);
        if (latest == null || !latest.encode().equals(taskSnapshot.encode())) {
            return false;
        }
        repository.upsertBatch(List.of(latest));
        return buffer.acknowledgeIfUnchanged(latest);
    }

    @Override
    public void destroy() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private record ProgressDelayTask(ProgressSnapshot snapshot, long deadlineNanos) implements Delayed {
        private ProgressDelayTask(ProgressSnapshot snapshot, Duration delay) {
            this(snapshot, System.nanoTime() + delay.toNanos());
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(deadlineNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }
    }
}
