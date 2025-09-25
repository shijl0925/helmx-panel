package com.helmx.tutorial.docker.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageBuildTask {
    private String taskId;
    private String imageName;
    private String status; // PENDING, RUNNING, SUCCESS, FAILED
    private String message;
    private StringBuilder streamBuilder; // 用于存储构建输出
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 获取字符串形式的流输出
    public String getStream() {
        return streamBuilder != null ? streamBuilder.toString() : "";
    }

    // 设置字符串形式的流输出（用于兼容性）
    public void setStream(String stream) {
        if (streamBuilder == null) {
            streamBuilder = new StringBuilder();
        }
        streamBuilder.setLength(0); // 清空现有内容
        streamBuilder.append(stream);
    }
}
