package com.helmx.tutorial.docker.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImagePushTaskManager {
    private final Map<String, ImagePushTask> tasks = new ConcurrentHashMap<>();

    public void addTask(String taskId, ImagePushTask task) {
        tasks.put(taskId, task);
    }

    public ImagePushTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void removeTask(String taskId) {
        tasks.remove(taskId);
    }
}
