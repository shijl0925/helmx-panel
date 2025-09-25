package com.helmx.tutorial.docker.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

@Component
public class ImageBuildTaskManager {
    private final Map<String, ImageBuildTask> tasks = new ConcurrentHashMap<>();

    public void addTask(String taskId, ImageBuildTask task) {
        tasks.put(taskId, task);
    }

    public ImageBuildTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void removeTask(String taskId) {
        tasks.remove(taskId);
    }
}
