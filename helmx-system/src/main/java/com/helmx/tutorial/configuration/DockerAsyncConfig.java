package com.helmx.tutorial.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DockerAsyncConfig {

    /**
     * 专用线程池，用于执行 Docker 镜像构建、拉取、推送等 I/O 密集型异步任务。
     * 使用独立线程池而非 ForkJoinPool 公共池，可以：
     * 1. 避免阻塞 ForkJoinPool 公共池，防止影响其他并行计算任务；
     * 2. 对线程数量、队列容量和拒绝策略进行独立配置；
     * 3. 通过有意义的线程命名（docker-task-N）方便问题排查。
     */
    @Bean(name = "dockerTaskExecutor")
    public Executor dockerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("docker-task-");
        executor.setKeepAliveSeconds(60);
        // 当线程池和队列均已满时，由调用方线程同步执行，避免丢失任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
