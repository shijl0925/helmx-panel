package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.utils.BaseTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImageBuildTask extends BaseTask {
    private String imageName;
    private StringBuilder streamBuilder; // 用于存储构建输出

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
