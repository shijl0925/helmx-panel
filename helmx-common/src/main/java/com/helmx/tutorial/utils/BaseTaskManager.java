package com.helmx.tutorial.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public abstract class BaseTaskManager<T extends BaseTask> {
    private static final Logger log = LoggerFactory.getLogger(BaseTaskManager.class);

    protected final Map<String, T> tasks = new ConcurrentHashMap<>();

    public void addTask(String taskId, T task) {
        tasks.put(taskId, task);
    }

    public T getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void removeTask(String taskId) {
        tasks.remove(taskId);
    }

    public boolean hasTask(String taskId) {
        return tasks.containsKey(taskId);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * 定时清理已完成超过1小时的任务，防止内存持续增长。
     * 每5分钟执行一次。
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupCompletedTasks() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusHours(1);
        AtomicInteger removed = new AtomicInteger(0);
        tasks.entrySet().removeIf(entry -> {
            T task = entry.getValue();
            boolean expired = task.isCompleted()
                    && task.getEndTime() != null
                    && task.getEndTime().isBefore(expiryThreshold);
            if (expired) {
                removed.incrementAndGet();
            }
            return expired;
        });
        if (removed.get() > 0) {
            log.info("[{}] Cleaned up {} completed task(s). Remaining: {}",
                    getClass().getSimpleName(), removed.get(), tasks.size());
        }
    }
}
