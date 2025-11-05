package com.helmx.tutorial.docker.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public abstract class BaseTaskManager<T extends BaseTask> {
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
}
