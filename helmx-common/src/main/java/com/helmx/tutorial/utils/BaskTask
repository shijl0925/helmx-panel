package com.helmx.tutorial.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseTask {
    protected String taskId;
    protected String status; // PENDING, RUNNING, SUCCESS, FAILED
    protected String message;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public boolean isCompleted() {
        return "SUCCESS".equals(status) || "FAILED".equals(status);
    }
}
