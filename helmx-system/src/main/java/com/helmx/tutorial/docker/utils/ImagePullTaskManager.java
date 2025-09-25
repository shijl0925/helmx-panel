package com.helmx.tutorial.docker.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

@Component
public class ImagePullTaskManager {
    private final Map<String, ImagePullTask> tasks = new ConcurrentHashMap<>();

    public void addTask(String taskId, ImagePullTask task) {
        tasks.put(taskId, task);
    }

    public ImagePullTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void removeTask(String taskId) {
        tasks.remove(taskId);
    }
}
