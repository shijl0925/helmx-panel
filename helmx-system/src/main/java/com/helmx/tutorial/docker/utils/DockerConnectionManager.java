package com.helmx.tutorial.docker.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DockerConnectionManager {

    @Value("${docker.apiVersion:1.41}")
    private String apiVersion;

    @Value("${docker.connection.timeout:30}")
    private int connectionTimeout;

    @Value("${docker.response.timeout:60}")
    private int responseTimeout;

    @Value("${docker.cert.upload.path}")
    private String dockerCertPath;

    @Autowired
    private DockerEnvMapper dockerEnvMapper;

    private final Map<String, DockerClient> clientCache = new ConcurrentHashMap<>();

//    // 健康状态缓存 (host -> (lastCheckTime, isHealthy))
//    private final Map<String, HealthStatus> healthCache = new ConcurrentHashMap<>();
//
//    // 健康检查缓存时间 (10秒)
//    private static final long HEALTH_CACHE_TTL = 10000;

    private boolean getTlsVerify(String host) {
        LambdaQueryWrapper<DockerEnv> queryWrapper = new LambdaQueryWrapper<>();
        DockerEnv dockerEnv = dockerEnvMapper.selectOne(queryWrapper.eq(DockerEnv::getHost, host));
        return dockerEnv != null ? dockerEnv.getTlsVerify() : false;
    }

    public DockerClient getDockerClient(String host) {
        String key = host + "_" + this.getTlsVerify(host);
        return clientCache.computeIfAbsent(key, k -> createDockerClient(host));
    }

    private DockerClient createDockerClient(String host) {
        try {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(host)
                    .withApiVersion(apiVersion);

            LambdaQueryWrapper<DockerEnv> queryWrapper = new LambdaQueryWrapper<>();
            DockerEnv dockerEnv = dockerEnvMapper.selectOne(queryWrapper.eq(DockerEnv::getHost, host));
            boolean tlsVerify = dockerEnv.getTlsVerify();

            // 配置TLS
            if (tlsVerify) {
                String envName = dockerEnv.getName().replaceAll("[/\\\\]", "");
                String certPath = Paths.get(dockerCertPath, envName, "certs").toString();
                configBuilder.withDockerTlsVerify(true)
                        .withDockerCertPath(certPath);
            } else {
                configBuilder.withDockerTlsVerify(false);
            }

            DefaultDockerClientConfig config = configBuilder.build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(URI.create(host))
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(connectionTimeout))
                    .responseTimeout(Duration.ofSeconds(responseTimeout))
                    .sslConfig(config.getSSLConfig())
                    .build();

            DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

            // 异步验证连接，避免阻塞
            CompletableFuture.supplyAsync(() -> {
                try {
                    dockerClient.infoCmd().exec();
                    return true;
                } catch (Exception e) {
                    log.warn("Failed to validate Docker connection to {}: {}", host, e.getMessage());
                    return false;
                }
            });
            return dockerClient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Docker daemon at " + host, e);
        }
    }

    public void removeClient(String host) {
        String key = host + "_" + this.getTlsVerify(host);
        clientCache.remove(key);
//        healthCache.remove(key);
    }

    // 添加连接健康检查方法
    public boolean checkConnectionHealth(String host) {
        String key = host + "_" + this.getTlsVerify(host);

        try {
            DockerClient client = clientCache.get(key);
            if (client != null) {
                // 使用 ping 命令，比 infoCmd 更轻量
                client.pingCmd().exec();
                return true;
            }
            return false;
        } catch (Exception e) {
//            log.debug("Connection health check failed for host: {}", host, e);
            return false;
        }
    }

//    // 健康状态内部类
//    private static class HealthStatus {
//        final long timestamp;
//        final boolean isHealthy;
//
//        HealthStatus(boolean isHealthy) {
//            this.timestamp = System.currentTimeMillis();
//            this.isHealthy = isHealthy;
//        }
//
//        boolean isExpired() {
//            return System.currentTimeMillis() - timestamp > HEALTH_CACHE_TTL;
//        }
//    }

//    public boolean isConnectionHealthy(String host) {
//        // 首先检查缓存
//        HealthStatus cachedStatus = healthCache.get(host);
//        if (cachedStatus != null && !cachedStatus.isExpired()) {
//            return cachedStatus.isHealthy;
//        }
//
//        // 缓存过期或不存在，执行实际检查
//        boolean isHealthy = checkConnectionHealth(host);
//
//        // 更新缓存
//        healthCache.put(host, new HealthStatus(isHealthy));
//
//        return isHealthy;
//    }

//    // 添加清理过期连接的方法
//    public void cleanupStaleConnections() {
//        // 可以根据需要实现连接清理逻辑
//        clientCache.entrySet().removeIf(entry -> {
//            try {
//                entry.getValue().infoCmd().exec();
//                return false;
//            } catch (Exception e) {
//                return true;
//            }
//        });
//    }
}
