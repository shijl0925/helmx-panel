package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.utils.BaseTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentLinkedQueue;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImageBuildTask extends BaseTask {
    private String imageName;
    private StringBuilder streamBuilder; // 用于存储完整构建输出（供轮询接口使用）

    /**
     * 实时日志队列：每条新日志行追加至此队列，供 SSE 流式接口逐行消费。
     * 使用无界线程安全队列，生产者（构建回调线程）写入，
     * 消费者（SSE 推送线程）通过 {@link ConcurrentLinkedQueue#poll()} 逐条取出。
     */
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();

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

    /**
     * 追加一行日志：同时写入全量历史 {@code streamBuilder}（带长度限制）
     * 以及实时队列 {@code logQueue}，保证两套消费路径均能获取到新日志。
     */
    public void appendToLog(String line) {
        // 写入实时队列供 SSE 消费
        logQueue.offer(line);

        // 写入全量历史，使用同步块保证 streamBuilder 线程安全
        synchronized (this) {
            if (streamBuilder == null) {
                streamBuilder = new StringBuilder();
            }
            streamBuilder.append(line);
            // 限制最大长度防止内存溢出
            if (streamBuilder.length() > 100000) {
                streamBuilder.delete(0, streamBuilder.length() - 80000);
            }
        }
    }
}
