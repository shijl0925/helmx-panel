package com.helmx.tutorial.docker.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImagePushTask {
    private String taskId;
    private String imageName;
    private String status; // PENDING, RUNNING, SUCCESS, FAILED
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}