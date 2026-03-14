package com.helmx.tutorial.docker.utils;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.helmx.tutorial.docker.dto.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.InvocationBuilder;
import com.alibaba.fastjson2.JSONObject;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.helmx.tutorial.docker.utils.GitUtil;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class DockerClientUtil {

    private static final long HOST_METRICS_CACHE_TTL_MILLIS = 30_000L;
    private final Object diskMetricsLock = new Object();
    private final Object localAddressesLock = new Object();

    @Autowired
    private DockerConnectionManager connectionManager;

    @Autowired
    private ImagePullTaskManager imagePullTaskManager;

    @Autowired
    private ImagePushTaskManager imagePushTaskManager;

    @Autowired
    private ImageBuildTaskManager imageBuildTaskManager;

    private final ThreadLocal<String> currentHost = new ThreadLocal<>();

    @Autowired
    private RegistryMapper registryMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private DockerHostValidator dockerHostValidator;

    @Autowired
    private DockerEnvMapper dockerEnvMapper;

    @Autowired
    private RemoteHostMetricsCollector remoteHostMetricsCollector;

    private volatile long diskMetricsLoadedAt;
    private volatile long cachedTotalDisk;
    private volatile long cachedUsableDisk;
    private volatile long previousDiskReadBytes;
    private volatile long previousDiskWriteBytes;
    private volatile long previousDiskSampleAt;
    private volatile long localAddressesLoadedAt;
    private volatile boolean localAddressesLoaded;
    private volatile Set<String> cachedLocalAddresses = Set.of();

    // 设置当前操作的服务器
    public void setCurrentHost(String host) {
        currentHost.remove();
        dockerHostValidator.validateHostAllowlist(host);
        currentHost.set(host);
    }

    // 清除当前线程的Docker主机，避免ThreadLocal泄漏
    public void clearCurrentHost() {
        currentHost.remove();
    }

    // 获取当前DockerClient
    public DockerClient getCurrentDockerClient() {
        String host = currentHost.get();
        if (host == null) {
            throw new IllegalStateException("No Docker host specified. Please call setCurrentHost() first.");
        }

        // 检查连接是否仍然有效
        boolean isHealthy = connectionManager.checkConnectionHealth(host);
//        boolean isHealthy = connectionManager.isConnectionHealthy(host);
        log.info("host: {} isHealthy: {}", host, isHealthy);
        if (!isHealthy) {
            // 尝试重新创建连接
            connectionManager.removeClient(host);
        }

        return connectionManager.getDockerClient(host);
    }

    // 获取当前DockerHttpClient（用于调用 docker-java 库未封装的 Docker API）
    public com.github.dockerjava.transport.DockerHttpClient getCurrentDockerHttpClient() {
        String host = currentHost.get();
        if (host == null) {
            throw new IllegalStateException("No Docker host specified. Please call setCurrentHost() first.");
        }
        return connectionManager.getDockerHttpClient(host);
    }

    // 获取当前Docker连接状态
    public boolean isConnectionHealthy() {
        String host = currentHost.get();
        if (host == null) {
            return false;
        }

        try {
            DockerClient client = getCurrentDockerClient();
            try (var cmd = client.pingCmd()) {
                cmd.exec();
            }
            return true;
        } catch (Exception e) {
            log.debug("Connection health check failed for host: {}", host, e);
            return false;
        }
    }

    public Info getInfo() {
        try (InfoCmd cmd = getCurrentDockerClient().infoCmd()) {
            return cmd.exec();
        }
    }

    public List<Container> listContainers() {
        try (ListContainersCmd cmd = getCurrentDockerClient().listContainersCmd().withShowAll(true)) {
            return cmd.exec();
        }
    }

    /**
     * 搜索容器
     */
    public List<Container> searchContainers(ContainerQueryRequest criteria) {
        try (ListContainersCmd cmd = getCurrentDockerClient().listContainersCmd().withShowAll(true)) {

            // 容器ID
            if (criteria.getContainerId() != null && !criteria.getContainerId().isEmpty()) {
                cmd.withIdFilter(Collections.singletonList(criteria.getContainerId()));
            }
            // 容器名称
            if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                cmd.withNameFilter(Collections.singletonList(criteria.getName()));
            }
            // 容器状态
            if (criteria.getState() != null && !criteria.getState().isEmpty()) {
                cmd.withStatusFilter(Collections.singletonList(criteria.getState()));
            }
            // 过滤器
            if (criteria.getFilters() != null && !criteria.getFilters().isEmpty()) {
                criteria.getFilters().entrySet().stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                        .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                        .forEach(entry -> cmd.withFilter(entry.getKey(), Collections.singleton(entry.getValue())));
            }

            return cmd.exec();
        }
    }

    /**
     * 获取容器状态
     *
     */
    public JSONObject getContainerStats(String containerId, boolean noStream) {
        try (StatsCmd cmd = getCurrentDockerClient().statsCmd(containerId).withNoStream(noStream)) {
            Statistics stats = cmd.exec(
                    new InvocationBuilder.AsyncResultCallback<Statistics>() {
                        @SneakyThrows
                        @Override
                        public void onNext(Statistics object) {
                            super.onNext(object);
                            super.close();
                        }
                    }
            ).awaitResult();

            return toJSON(stats);
        }
    }

    /**
     * 获取容器进程列表
     */
    public JSONObject getContainerTop(String containerId) {
        try (TopContainerCmd cmd = getCurrentDockerClient().topContainerCmd(containerId)) {
            return toJSON(cmd.exec());
        }
    }

    /**
     * 获取容器详情
     */
    public InspectContainerResponse inspectContainer(String containerId) {
        try (InspectContainerCmd cmd = getCurrentDockerClient().inspectContainerCmd(containerId)) {
            return cmd.exec();
        }
    }

    /**
     * 通用Docker操作执行器
     */
    private Map<String, Object> executeDockerOperation(DockerOperation operation, String errorPrefix) {
        Map<String, Object> result = new HashMap<>();
        try {
            String successMessage = operation.execute();
            result.put("status", "success");
            result.put("message", successMessage);
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("message", errorPrefix + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Docker操作函数式接口
     */
    @FunctionalInterface
    private interface DockerOperation {
        String execute() throws Exception;
    }

    /**
     * 启动容器
     */
    public Map<String, Object> startContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (StartContainerCmd cmd = getCurrentDockerClient().startContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container start successfully";
        }, "Failed to start container");
    }

    /**
     * 停止容器
     */
    public Map<String, Object> stopContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (StopContainerCmd cmd = getCurrentDockerClient().stopContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container stop successfully";
        }, "Failed to stop container");
    }

    /**
     * 重启容器
     */
    public Map<String, Object> restartContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (RestartContainerCmd cmd = getCurrentDockerClient().restartContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container restart successfully";
        }, "Failed to restart container");
    }

    /**
     * 删除容器
     */
    public Map<String, Object> removeContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (RemoveContainerCmd cmd = getCurrentDockerClient().removeContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container remove successfully";
        }, "Failed to remove container");
    }

    /**
     * 强制删除容器
     */
    public Map<String, Object> removeContainerForce(String containerId) {
        return executeDockerOperation(() -> {
            try (RemoveContainerCmd cmd = getCurrentDockerClient().removeContainerCmd(containerId).withForce(true)) {
                cmd.exec();
            }
            return "Container force-removed successfully";
        }, "Failed to force-remove container");
    }

    /**
     * 杀死容器
     */
    public Map<String, Object> killContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (KillContainerCmd cmd = getCurrentDockerClient().killContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container kill successfully";
        }, "Failed to kill container");
    }

    /**
     * 暂停容器
     */
    public Map<String, Object>  pauseContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (PauseContainerCmd cmd = getCurrentDockerClient().pauseContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container pause successfully";
        }, "Failed to pause container");
    }

    /**
     * 恢复容器
     */
    public Map<String, Object> unpauseContainer(String containerId) {
        return executeDockerOperation(() -> {
            try (UnpauseContainerCmd cmd = getCurrentDockerClient().unpauseContainerCmd(containerId)) {
                cmd.exec();
            }
            return "Container unpause successfully";
        }, "Failed to unpause container");
    }

    private static final int MAX_LOG_SIZE_BYTES = 1048576; // 1MB
    private static final int LOG_RETAIN_SIZE_BYTES = 524288; // 512KB

    // Constants for temporary volume operations
    private static final String VOLUME_HELPER_IMAGE = "busybox:latest";
    private static final String VOLUME_BACKUP_MOUNT_PATH = "/backup-data";
    private static final String VOLUME_BACKUP_CONTAINER_PREFIX = "helmx-backup-";
    private static final String VOLUME_CLONE_SRC_MOUNT = "/source";
    private static final String VOLUME_CLONE_DST_MOUNT = "/target";
    private static final String VOLUME_CLONE_CONTAINER_PREFIX = "helmx-clone-";
    private static final long VOLUME_CLONE_TIMEOUT_SECONDS = 60L;
    private static final String DEFAULT_BIND_IP = "0.0.0.0";
    private static final String DEFAULT_PORT_TYPE = "tcp";

    /**
     * 获取容器日志
     */
    public String getContainerLogs(String containerId, int tailNum) {
        StringBuilder logs = new StringBuilder();

        try (LogContainerCmd cmd = getCurrentDockerClient().logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)) {

            if (tailNum > 0) {
                cmd.withTail(tailNum);
            } else {
                cmd.withTailAll();
            }

            cmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    if (item != null && item.getPayload() != null) {
                        logs.append(new String(item.getPayload(), StandardCharsets.UTF_8)).append("\n");
                        // 限制日志长度，避免内存占用过高
                        if (logs.length() > MAX_LOG_SIZE_BYTES) {
                            logs.delete(0, logs.length() - LOG_RETAIN_SIZE_BYTES);
                        }
                    }
                }
            }).awaitCompletion(3000, TimeUnit.MILLISECONDS);
            return logs.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 保留中断状态
            throw new RuntimeException("获取容器日志被中断", e);
        } catch (Exception e) {
            throw new RuntimeException("获取容器日志失败", e);
        }
    }

    public Map<String, Object> renameContainer(ContainerRenameRequest criteria) {
        Map<String, Object> result = new HashMap<>();
        String containerId = criteria.getContainerId();
        String newName = criteria.getNewName();

        try (RenameContainerCmd cmd = getCurrentDockerClient().renameContainerCmd(containerId)) {
            // 验证容器名称
            if (newName == null || newName.isEmpty()) {
                throw new IllegalArgumentException("New container name cannot be null or empty");
            }

            if (!isValidContainerName(newName)) {
                throw new IllegalArgumentException("Container name: " + newName +
                        ". only [a-zA-Z0-9][a-zA-Z0-9_.-] are allowed");
            }

            // 执行重命名操作
            cmd.withName(newName).exec();

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container renamed successfully");
            log.info("Renamed container {} to {}", containerId, newName);
        } catch (Exception e) {

            String errorMsg = "Failed to rename container: " + e.getMessage();
            result.put("status", "failed");
            result.put("message", errorMsg);
            log.error("Failed to rename container", e);
        }

        return result;
    }

    /**
     * 从镜像名称中提取注册表URL
     */
    private String extractRegistryUrl(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return null;
        }

        // 处理带标签的情况，先去掉标签部分
        String imageNameWithoutTag = imageName;
        if (imageName.contains(":")) {
            int lastColonIndex = imageName.lastIndexOf(":");
            String afterColon = imageName.substring(lastColonIndex + 1);
            // 如果冒号后面不是纯数字或者数字长度超过5位，则认为是标签而不是端口号
            if (!afterColon.matches("\\d{1,5}")) {
                imageNameWithoutTag = imageName.substring(0, lastColonIndex);
            }
        }

        // 如果不包含斜杠，是官方镜像或单级镜像名
        if (!imageNameWithoutTag.contains("/")) {
            return null; // DockerHub 官方镜像
        }

        // 包含斜杠的情况
        String[] parts = imageNameWithoutTag.split("/");

        // 如果只有两个部分 (namespace/repository)
        if (parts.length == 2) {
            String firstPart = parts[0];

            // 如果第一部分包含点号或端口号，则是私有注册表
            if (firstPart.contains(".") || firstPart.contains(":")) {
                return firstPart.startsWith("http") ? firstPart : "https://" + firstPart;
            }
            // 否则认为是 DockerHub 用户镜像
            return null;
        }

        // 如果多于两个部分，第一部分应该是注册表地址
        if (parts.length > 2) {
            String hostPart = parts[0];
            // 验证是否为有效的主机地址
            if (hostPart.contains(".") || hostPart.contains(":")) {
                if (!hostPart.startsWith("http")) {
                    // 判断协议
                    if (hostPart.startsWith("localhost") ||
                            hostPart.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+.*") ||
                            hostPart.startsWith("127.0.0.1")) {
                        return "http://" + hostPart;
                    }
                    return "https://" + hostPart;
                }
                return hostPart;
            }
        }

        return null; // 默认为 DockerHub
    }


    /**
     * 根据镜像名称获取认证配置
     */
    private AuthConfig getAuthConfigForImage(String imageName) {
        try {
            // 解析镜像名称获取仓库地址
            String registryUrl = extractRegistryUrl(imageName);
            if (registryUrl == null) {
                // 默认使用 DockerHub，如果需要认证则从配置中获取
                registryUrl = "https://docker.io";
                // DockerHub 用户私有仓库需要使用这个特殊的 registry URL: https://index.docker.io/v1/
            }
            log.info("Registry URL: {}", registryUrl);

            // 从数据库获取注册表信息
            QueryWrapper<Registry> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("url", registryUrl);
            List<Registry> registries = registryMapper.selectList(queryWrapper);

            if (registries.size() > 1) {
                log.warn("Multiple registries found for URL: {}", registryUrl);
            }
            Registry registry = registries.isEmpty() ? null : registries.get(0);

            if (registry != null && registry.getAuth() != null && registry.getAuth()) {
                return new AuthConfig()
                        .withRegistryAddress(registry.getUrl())
                        .withUsername(registry.getUsername())
                        .withPassword(passwordUtil.decrypt(registry.getPassword()));
            }
        } catch (Exception e) {
            log.warn("Failed to get auth config for image: {}", imageName, e);
        }
        return null;
    }

    /**
     * 拉取镜像（如果不存在）
     */
    public Map<String, String> pullImageIfNotExists(String imageName, Boolean checkIfExists) throws InterruptedException {
        Map<String, String> result = new HashMap<>();
        result.put("imageName", imageName);

        String taskId = UUID.randomUUID().toString();
        result.put("taskId", taskId);

        // 创建任务
        ImagePullTask task = new ImagePullTask();
        task.setTaskId(taskId);
        task.setImageName(imageName);
        task.setStatus("PENDING");
        task.setStartTime(LocalDateTime.now());
        // 添加任务
        imagePullTaskManager.addTask(taskId, task);

        DockerClient client = getCurrentDockerClient();

        if (checkIfExists != null && checkIfExists) {
            try (InspectImageCmd cmd = client.inspectImageCmd(imageName)) {
                // 检查镜像是否存在
                cmd.exec();
                log.info("Image {} already exists", imageName);

                task.setStatus("SUCCESS");
                task.setMessage("镜像拉取成功");
                task.setEndTime(LocalDateTime.now());

                return result; // 如果镜像存在，直接返回
            } catch (Exception e) {
                // 镜像不存在，继续执行拉取操作
                log.info("Image {} not found, pulling...", imageName);
            }
        }

        // 镜像不存在，异步执行拉取操作
        CompletableFuture.runAsync(() -> {
            try {
                // 检查是否需要认证
                AuthConfig authConfig = getAuthConfigForImage(imageName);

                try (PullImageCmd cmd = client.pullImageCmd(imageName)) {
                    // 如果需要认证，设置认证信息
                    if (authConfig != null) {
                        cmd.withAuthConfig(authConfig);
                    }

                    log.info("Pulling image: {}", imageName);
                    PullImageResultCallback callback = new PullImageResultCallback() {
                        @Override
                        public void onNext(PullResponseItem item) {
                            log.info("Pull progress: {}", item.getStatus());
                            task.setMessage(item.getStatus());
                            super.onNext(item);
                        }
                    };

                    task.setStatus("RUNNING");
                    task.setMessage("镜像拉取中...");
                    cmd.exec(callback).awaitCompletion();

                    // 镜像拉取完成, 更新任务成功状态
                    task.setStatus("SUCCESS");
                    task.setMessage("镜像拉取成功");
                    task.setEndTime(LocalDateTime.now());

                    log.info("Successfully pulled image: {}", imageName);
                }
            } catch (InterruptedException e) {
                // 线程被中断, 更新任务异常状态
                task.setStatus("FAILED");
                task.setMessage("镜像拉取失败：线程被中断");

                log.error("Error pulling image: 线程被中断");
                Thread.currentThread().interrupt(); // 保留中断状态
            } catch (Exception e) {
                // 其他异常, 更新任务异常状态
                task.setStatus("FAILED");
                task.setMessage("镜像拉取失败：" + e.getMessage());

                log.error("Error pulling image: {}", e.getMessage());
            }
        });
        return result;
    }

    /**
     * 为镜像打标签
     */
    public Map<String, Object> tagImage(String imageId, String tagImageName) {
        Map<String, Object> result = new HashMap<>();
        // Use lastIndexOf to correctly handle image names with registry port (e.g., registry:5000/image:tag)
        int colonIdx = tagImageName.lastIndexOf(':');
        String imageNameWithRepository;
        String tag;
        if (colonIdx > 0 && colonIdx < tagImageName.length() - 1
                && !tagImageName.substring(colonIdx + 1).contains("/")) {
            imageNameWithRepository = tagImageName.substring(0, colonIdx);
            tag = tagImageName.substring(colonIdx + 1);
        } else {
            imageNameWithRepository = tagImageName;
            tag = "latest";
        }
        try (TagImageCmd cmd = getCurrentDockerClient().tagImageCmd(imageId, imageNameWithRepository, tag)) {
            cmd.exec();

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container renamed successfully");
            log.info("Image tagged successfully: imageId={}, imageNameWithRepository={}, tag={}",
                    imageId, imageNameWithRepository, tag);
        } catch (Exception e) {

            String errorMsg = "Failed to tag image: " + e.getMessage();
            result.put("status", "failed");
            result.put("message", errorMsg);
            log.error("Failed to tag image: imageId={}, imageNameWithRepository={}, tag={}",
                    imageId, imageNameWithRepository, tag, e);
        }

        return result;
    }

    // 抽取端口协议处理逻辑
    private ExposedPort createExposedPort(int containerPort, String protocol) {
        return switch (protocol != null ? protocol.toLowerCase() : "tcp") {
            case "sctp" -> ExposedPort.sctp(containerPort);
            case "udp" -> ExposedPort.udp(containerPort);
            default -> ExposedPort.tcp(containerPort);
        };
    }

    /**
     * 验证容器名称是否符合Docker命名规范
     * @param name 容器名称
     * @return true 如果名称有效，否则 false
     */
    private boolean isValidContainerName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Docker容器名称规则：
        // 1. 必须以字母、数字开头
        // 2. 只能包含字母、数字、下划线(_)、点(.)、连字符(-)
        // 3. 长度限制通常为128个字符
        return name.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$");
    }

    // 解析重启策略
    private RestartPolicy parseRestartPolicy(RestartPolicyRequest rp) {
        String restartPolicyName = rp.getName();
        log.info("restartPolicyName: {}", restartPolicyName);
        try {
            if ("no".equals(restartPolicyName)) {
                return RestartPolicy.noRestart();
            } else if ("always".equals(restartPolicyName)) {
                return RestartPolicy.alwaysRestart();
            } else if (restartPolicyName.startsWith("on-failure")) {
                Integer maxRetries = rp.getMaximumRetryCount();
                log.info("maxRetries: {}", maxRetries);
                // 只有 "on-failure"，使用 0 表示无限制重启
                return RestartPolicy.onFailureRestart(Objects.requireNonNullElse(maxRetries, 0));
            } else if ("unless-stopped".equals(restartPolicyName)) {
                return RestartPolicy.unlessStoppedRestart();
            }
        } catch (Exception e) {
            log.warn("Invalid restart policy: {}, using default", restartPolicyName);
        }

        return null;
    }

    /**
     * 配置容器参数
     */
    private void configureContainer(CreateContainerCmd createContainerCmd, ContainerCreateRequest criteria) {
        // 设置命令参数(cmd)（如果提供）
        if (criteria.getCmd() != null && criteria.getCmd().length > 0 ) {
            createContainerCmd.withCmd(criteria.getCmd());
        }

        // 设置启动命令参数(entrypoint)（如果提供）
        if (criteria.getEntrypoint() != null && criteria.getEntrypoint().length > 0) {
            createContainerCmd.withEntrypoint(criteria.getEntrypoint());
        }

        // 设置环境变量（如果提供）
        if (criteria.getEnv() != null && criteria.getEnv().length > 0 ) {
            createContainerCmd.withEnv(criteria.getEnv());
        }

        // 设置暴露端口（如果提供）
        PortHelper[] exposedPortsArray = criteria.getExposedPorts();
        if (exposedPortsArray != null && exposedPortsArray.length > 0) {
            List<ExposedPort> exposedPorts = new ArrayList<>();
            for (PortHelper ph : exposedPortsArray) {
                int containerPort = Integer.parseInt(ph.getContainerPort());
                // 根据协议创建 ExposedPort
                String protocol = ph.getProtocol();
                ExposedPort exposedPort = createExposedPort(containerPort, protocol);
                exposedPorts.add(exposedPort);
            }
            createContainerCmd.withExposedPorts(exposedPorts);
        }

//        // 配置容器是否自动发布所有端口
//        if (criteria.getPublishAllPorts() != null) {
//            createContainerCmd.withPublishAllPorts(criteria.getPublishAllPorts());
//        }

        // 配置容器健康检查
        Map<String, Object> hc = criteria.getHealthCheck();
        if (hc != null) {
            HealthCheck healthCheck = new HealthCheck();

            if (hc.get("test") != null && hc.get("test") instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> testList = (List<String>) hc.get("test");
                    healthCheck.withTest(testList);
                } catch (ClassCastException e) {
                    // 处理类型转换异常
                    throw new IllegalArgumentException("Health check test parameter must be a List<String>", e);
                }
            }
            if (hc.get("interval") != null && hc.get("interval") instanceof Long interval) {
                if (interval > 0) {
                    healthCheck.withInterval(interval);
                }
            }
            if (hc.get("timeout") != null && hc.get("timeout") instanceof Long timeout) {
                if (timeout > 0) {
                    healthCheck.withTimeout(timeout);
                }
            }
            if (hc.get("retries") != null && hc.get("retries") instanceof Integer retries) {
                if (retries > 0) {
                    healthCheck.withRetries(retries);
                }
            }
            if (hc.get("startPeriod") != null && hc.get("startPeriod") instanceof Long startPeriod) {
                if (startPeriod > 0) {
                    healthCheck.withStartPeriod(startPeriod);
                }
            }
            if (hc.get("startInterval") != null && hc.get("startInterval") instanceof Long startInterval) {
                if (startInterval > 0) {
                    healthCheck.withStartInterval(startInterval);
                }
            }
            createContainerCmd.withHealthcheck(healthCheck);
        }

        // 设置输入
        if (criteria.getStdinOpen() != null) {
            createContainerCmd.withStdinOpen(criteria.getStdinOpen());
        }

        if (criteria.getTty() != null) {
            createContainerCmd.withTty(criteria.getTty());
        }

        // 设置主机名
        if (criteria.getHostName() != null && !criteria.getHostName().isEmpty()) {
            createContainerCmd.withHostName(criteria.getHostName());
        }

        // 设置Domain Name
        String domainName = criteria.getDomainName();
        if (domainName != null && !domainName.isEmpty()) {
            createContainerCmd.withDomainName(domainName);
        }

        // 设置MAC 地址
        String macAddress = criteria.getMacAddress();
        if (macAddress != null && !macAddress.isEmpty()) {
            createContainerCmd.withMacAddress(macAddress);
        }

        // 配置IPv4 地址
        String ipv4Address = criteria.getIpv4Address();
        if (ipv4Address != null && !ipv4Address.isEmpty()) {
            createContainerCmd.withIpv4Address(ipv4Address);
        }

        // 配置IPv6 地址
        String ipv6Address = criteria.getIpv6Address();
        if (ipv6Address != null && !ipv6Address.isEmpty()) {
            createContainerCmd.withIpv6Address(ipv6Address);
        }

        // 设置标签
        String[] labels = criteria.getLabels();
        if (labels != null && labels.length > 0) {
            Map<String, String> labelMap = new HashMap<>();
            for (String label : labels) {
                if (label == null || !label.contains("=")) {
                    log.warn("Invalid label format: {}", label);
                    continue;
                }

                String[] keyValue = label.split("=", 2);
                labelMap.put(keyValue[0], keyValue[1]);
            }
            createContainerCmd.withLabels(labelMap);
        }

        // 设置主机配置
        HostConfig hostConfig = HostConfig.newHostConfig();

        // 设置端口绑定（如果提供）
        if (exposedPortsArray != null && exposedPortsArray.length > 0) {
            Ports portBindings = new Ports();
            for (PortHelper ph : exposedPortsArray) {
                // 获取主机端口(默认为空)
                Ports.Binding binding = Ports.Binding.empty();
                if (ph.getHostPort() != null && !ph.getHostPort().isEmpty()) {
                    binding = Ports.Binding.bindPort(Integer.parseInt(ph.getHostPort()));
                }

                // 获取容器端口
                int containerPort = Integer.parseInt(ph.getContainerPort());
                // 端口协议
                String protocol = ph.getProtocol();
                ExposedPort exposedPort = createExposedPort(containerPort, protocol);
                portBindings.bind(exposedPort, binding);
            }

            hostConfig.withPortBindings(portBindings);
        }

        if (criteria.getDevices() != null) {
            List<Device> devices = new ArrayList<>();
            for (Map.Entry<String, String> entry : criteria.getDevices().entrySet()) {
                String pathOnHost = entry.getKey();
                String[] parts = entry.getValue().split(":");
                String pathInContainer = parts[0];
//                cGroupPermissions 参数控制容器对设备的访问权限：r: 读权限, w: 写权限, m: 创建设备节点权限（mknod）
                String cGroupPermissions = parts.length > 1 ? parts[1] : "rwm";
                Device device = new Device(cGroupPermissions, pathInContainer, pathOnHost);
                devices.add(device);
            }

            hostConfig.withDevices(devices);
        }

        // 添加卷
        VolumeHelper[] volumes = criteria.getVolumes();
        if (volumes != null && volumes.length > 0) {
            List<Bind> binds = new ArrayList<>();
            for (VolumeHelper volume : volumes) {
                binds.add(new Bind(volume.getHostPath(), new Volume(volume.getContainerPath()), AccessMode.valueOf(volume.getMode())));
            }
            hostConfig.withBinds(binds);
        }

        // 设置特权模式
        if (criteria.getPrivileged() != null) {
            hostConfig.withPrivileged(criteria.getPrivileged());
        }

        // 删除容器时自动删除
        if (criteria.getAutoRemove() != null) {
            hostConfig.withAutoRemove(criteria.getAutoRemove());
        }

        // 设置 CPU 时间分配的相对权重, 取值范围：通常为 2-262144，默认值为 1024.
        // (这是一个相对权重值，不是绝对的 CPU 使用限制. 当系统 CPU 资源充足时，容器可以使用超过设定权重的 CPU.当 CPU 资源竞争时，按照权重比例分配 CPU 时.)
        if (criteria.getCpuShares() != null) {
            hostConfig.withCpuShares(criteria.getCpuShares());
        }

        // 设置容器可以使用的 CPU 核心数（以纳核为单位）
        // 单位：纳核（nano CPU），1 CPU 核心 = 1,000,000,000 纳核
        // 这是绝对的 CPU 使用限制
        // 容器最多只能使用指定数量的 CPU 核心
        // 更直观易懂，推荐使用
        if (criteria.getCpuNano() != null) {
            hostConfig.withNanoCPUs((long) (criteria.getCpuNano() * 1000000000L));
        }

        // 设置容器可以使用的最大内存量（以字节为单位）
        if (criteria.getMemory() != null) {
            hostConfig.withMemory((long) (criteria.getMemory() * 1024 * 1024));
        }

        // 设置容器的网络模式
        String networkMode = criteria.getNetworkMode();
        if (networkMode != null && !networkMode.isEmpty()) {
            hostConfig.withNetworkMode(networkMode);
        }

        // 设置 DNS 服务器
        String[] dns = criteria.getDns();
        if (dns != null && dns.length > 0) {
            hostConfig.withDns(dns);
        }

        // 设置重启策略
        if (criteria.getRestartPolicy() != null) {
            RestartPolicy restartPolicy = parseRestartPolicy(criteria.getRestartPolicy());
            if (restartPolicy != null) {
                hostConfig.withRestartPolicy(restartPolicy);
            }
        }

        createContainerCmd.withHostConfig(hostConfig);

        // 设置容器的工作目录
        String workingDir = criteria.getWorkingDir();
        if (workingDir != null && !workingDir.isEmpty()) {
            createContainerCmd.withWorkingDir(workingDir);
        }

        // 设置容器的用户
        String user = criteria.getUser();
        if (user != null && !user.isEmpty()) {
            createContainerCmd.withUser(user);
        }
    }

    /**
     * 创建新容器
     */
    public Map<String, Object> createContainer(ContainerCreateRequest criteria) {
        Map<String, Object> result = new HashMap<>();
        String containerId = null;

        try {
            String imageName = criteria.getImage();
            if (imageName == null || imageName.isEmpty()) {
                throw new IllegalArgumentException("Image name must not be null or empty for container creation.");
            }

            // 验证容器名称
            String containerName = criteria.getName();
            if (containerName != null && !containerName.isEmpty()) {
                if (!isValidContainerName(containerName)) {
                    throw new IllegalArgumentException("Container name: " + containerName +
                            ". only [a-zA-Z0-9][a-zA-Z0-9_.-] are allowed");
                }
            }

            // 创建容器命令
            try (CreateContainerCmd createContainerCmd = getCurrentDockerClient().createContainerCmd(imageName)) {
                if (containerName != null && !containerName.isEmpty()) {
                    createContainerCmd.withName(containerName);
                }
                log.info("Creating new container with image: {}", imageName);

                // 设置容器配置
                configureContainer(createContainerCmd, criteria);

                // 执行创建容器命令
                CreateContainerResponse container = createContainerCmd.exec();
                containerId = container.getId();
                log.info("Created new container with ID: {}", containerId);
            }

            // 启动新容器
            try (StartContainerCmd cmd = getCurrentDockerClient().startContainerCmd(containerId)) {
                cmd.exec();
            }
            log.info("Started new container with ID: {}", containerId);

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container created successfully");
            result.put("containerId", containerId);
        } catch (Exception e) {
            log.error("Failed to create container", e);

            result.put("status", "failed");
            String errorMsg = "Failed to create container: " + e.getMessage();
            result.put("message", errorMsg);

//            // 清理已创建但未能启动的容器
//            if (containerId != null) {
//                try {
//                    getCurrentDockerClient().removeContainerCmd(containerId).withForce(true).exec();
//                    log.info("Cleaned up failed container: {}", containerId);
//                } catch (Exception cleanupException) {
//                    log.warn("Failed to cleanup container during error handling", cleanupException);
//                }
//            }
        }

        return result;
    }

    public Map<String, Object> updateContainer(ContainerCreateRequest criteria) {
        Map<String, Object> result = new HashMap<>();

        String containerId = criteria.getContainerId();

        // 获取原容器的配置信息
        InspectContainerResponse containerInfo;
        try (InspectContainerCmd cmd = getCurrentDockerClient().inspectContainerCmd(containerId)) {
            containerInfo = cmd.exec();
        }
        String originalContainerName = containerInfo.getName().substring(1);
        log.info("Original Container Name：{}", originalContainerName);

        // 获取容器名称
        String newContainerName  = originalContainerName;
        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            newContainerName = criteria.getName();
            if (!isValidContainerName(newContainerName)) {
                throw new IllegalArgumentException("Container name: " + newContainerName +
                        ". only [a-zA-Z0-9][a-zA-Z0-9_.-] are allowed");
            }
        }
        log.info("New Container name: {}", newContainerName);

        try {
            // 确定要使用的镜像名称
            String newImageName;
            if (criteria.getImage() != null && !criteria.getImage().isEmpty()) {
                newImageName = criteria.getImage();
            } else {
                // 如果没有指定新镜像，则使用原镜像
                newImageName = containerInfo.getConfig().getImage();
            }
            log.info("New image name: {}", newImageName);

            // 停止容器
            try {
                // 检查容器是否正在运行再停止
                if (Boolean.TRUE.equals(containerInfo.getState().getRunning())) {
                    try (StopContainerCmd cmd = getCurrentDockerClient().stopContainerCmd(containerId)) {
                        cmd.exec();
                    }
                    log.info("Stopping container: {}", containerId);

                    // 使用 Docker 的等待命令等待容器停止
                    try (WaitContainerCmd waitCmd = getCurrentDockerClient().waitContainerCmd(containerId)) {
                        // 设置超时时间
                        waitCmd.start().awaitCompletion(30, TimeUnit.SECONDS);
                    }
                    log.info("Container {} has stopped completely", containerId);
                }
            } catch (Exception e) {
                // 容器可能已经停止
                log.warn("Container already stopped or stop failed: {}", containerId);
            }

            // 备份容器
            String backupName = originalContainerName + "_backup";
            try (RenameContainerCmd cmd = getCurrentDockerClient().renameContainerCmd(containerId)) {
                cmd.withName(backupName).exec();
            }
            log.info("Backup container with ID: {}, new container Name: {}", containerId, backupName);

            // 使用新配置创建容器
            if (newImageName == null || newImageName.isEmpty()) {
                throw new IllegalArgumentException("Image name must not be null or empty.");
            }
            String newContainerId;
            try (CreateContainerCmd createContainerCmd = getCurrentDockerClient().createContainerCmd(newImageName)
                    .withName(newContainerName)) {
                log.info("Creating container: {} with image: {}", newContainerName, newImageName);

                // 设置容器配置
                configureContainer(createContainerCmd, criteria);

                // 执行创建容器命令
                CreateContainerResponse newContainer = createContainerCmd.exec();
                newContainerId = newContainer.getId();
                log.info("Created new container with ID: {}", newContainerId);
            }

            // 启动新容器
            try (StartContainerCmd cmd = getCurrentDockerClient().startContainerCmd(newContainerId)) {
                cmd.exec();
            }
            log.info("Started new container with ID: {}", newContainerId);

            // 删除原容器
            try (RemoveContainerCmd cmd = getCurrentDockerClient().removeContainerCmd(containerId)) {
                cmd.exec();
            }
            log.info("Original container removed: {}", containerId);

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container updated successfully");
            result.put("containerId", containerId);
            result.put("newContainerId", newContainerId);
        } catch (Exception e) {
            // 回滚：如果新容器创建失败，尝试恢复旧容器
            log.error("Failed to update container. Rolling back to the previous container...", e);
            log.info("Rolling back to the previous container: {}", originalContainerName);

            result.put("status", "failed");
            String errorMsg = "Failed to update container: " + e.getMessage() + "Rolling back to the previous container.";
            result.put("message", errorMsg);
            result.put("containerId", containerId);

            try {
                // 删除可能创建失败的新容器
                log.info("Deleting the new container: {}", newContainerName);
                try {
                    InspectContainerResponse newContainerInfo;
                    try (InspectContainerCmd cmd = getCurrentDockerClient().inspectContainerCmd(newContainerName)) {
                        newContainerInfo = cmd.exec();
                    }

                    if (newContainerInfo != null) {
                        result.put("newContainerId", newContainerInfo.getId());
                        try (RemoveContainerCmd cmd = getCurrentDockerClient().removeContainerCmd(newContainerInfo.getId())) {
                            cmd.exec();
                        }
                        log.info("Remove Failed Container: {}", newContainerInfo.getId());
                    }
                } catch (Exception removeException) {
                    log.debug("No new container to remove during rollback");
                }

                // 恢复旧容器名称
                try (RenameContainerCmd cmd = getCurrentDockerClient().renameContainerCmd(containerId)) {
                    cmd.withName(originalContainerName).exec();
                }
                log.info("Rollback: Restored container {} with old name: {}", containerId, originalContainerName);

                // 启动旧容器
                try (StartContainerCmd cmd = getCurrentDockerClient().startContainerCmd(containerId)) {
                    cmd.exec();
                }

                log.info("Rollback successful. Old container has been restored.");
            } catch (Exception rollbackException) {
                log.error("Failed to roll back to the previous container.", rollbackException);
            }
        }

        return result;
    }

    public Map<String, Object> loadStatus() {
        long imageSize = 0;
        List<Image> images = this.listImages().stream()
                .filter(image -> image.getRepoTags() != null &&
                        Arrays.stream(image.getRepoTags()).noneMatch("<none>:<none>"::equals)).toList();

        for (Image image : images) {
            imageSize += image.getSize();
        }

        // 获取所有网络
        List<Network> networks;
        try (ListNetworksCmd cmd = getCurrentDockerClient().listNetworksCmd()) {
            networks = cmd.exec();
        }

        // 获取所有卷
        List<InspectVolumeResponse> volumes;
        try (ListVolumesCmd cmd = getCurrentDockerClient().listVolumesCmd()) {
            volumes = cmd.exec().getVolumes();
        }

        Map<String, Object> result = new HashMap<>(Map.of(
                "imageCount", images.size(),
                "imageSize", ByteUtils.formatBytes(imageSize),
                "networkCount", networks.size(),
                "volumeCount", volumes.size()
        ));

        // 获取所有容器
        List<Container> containers;
        try (ListContainersCmd cmd = getCurrentDockerClient().listContainersCmd().withShowAll(true)) {
            containers = cmd.exec();
        }
        result.put("containerCount", containers.size());

        // 一次遍历统计所有状态
        long created = 0, running = 0, paused = 0, stopped = 0, exited = 0, restarting = 0, removing = 0, dead = 0;
        for (Container container : containers) {
            String state = container.getState();
            if ("created".equals(state)) {
                created++;
            } else if ("running".equals(state)) {
                running++;
            } else if ("paused".equals(state)) {
                paused++;
            } else if ("stopped".equals(state)) {
                stopped++;
            } else if ("exited".equals(state)) {
                exited++;
            } else if ("restarting".equals(state)) {
                restarting++;
            } else if ("removing".equals(state)) {
                removing++;
            } else if ("dead".equals(state)) {
                dead++;
            }
        }

        result.put("created", created);
        result.put("running", running);
        result.put("paused", paused);
        result.put("stopped", stopped);
        result.put("exited", exited);
        result.put("restarting", restarting);
        result.put("removing", removing);
        result.put("dead", dead);

        result.putAll(loadHostResourceUsage());
        return result;
    }

    /**
     * Loads host resource metrics to enrich the System Info response.
     * <p>
     * Returned keys:
     * <ul>
     *     <li>{@code hostMetricsAvailable}: {@link Boolean}</li>
     *     <li>{@code hostMetricsDebug}: {@link String} explaining why metrics are unavailable or how they were collected</li>
     *     <li>{@code hostCpuUsage}, {@code hostMemoryUsage}, {@code hostDiskUsage}: {@link Double} percentages</li>
     *     <li>{@code hostMemoryUsed}, {@code hostMemoryTotal}, {@code hostDiskUsed}, {@code hostDiskTotal}: formatted {@link String} sizes</li>
     *     <li>{@code DiskReadTrafficNew}, {@code WriteTrafficNew}: {@link Double} KB/s disk read/write throughput</li>
     * </ul>
     * For local Docker hosts (for example {@code unix:///var/run/docker.sock}), the map contains live host metrics.
     * For remote Docker hosts, the method tries to use the matching DockerEnv SSH settings to collect metrics from the
     * target host; otherwise it keeps safe default values.
     */
    // package-private for focused unit tests of host metrics fallback behavior.
    Map<String, Object> loadHostResourceUsage() {
        Map<String, Object> hostMetrics = new HashMap<>();
        hostMetrics.put("hostMetricsAvailable", false);
        hostMetrics.put("hostCpuUsage", 0D);
        hostMetrics.put("hostMemoryUsage", 0D);
        hostMetrics.put("hostMemoryUsed", "0B");
        hostMetrics.put("hostMemoryTotal", "0B");
        hostMetrics.put("hostDiskUsage", 0D);
        hostMetrics.put("hostDiskUsed", "0B");
        hostMetrics.put("hostDiskTotal", "0B");
        hostMetrics.put("DiskReadTrafficNew", 0D);
        hostMetrics.put("WriteTrafficNew", 0D);
        hostMetrics.put("hostMetricsDebug", "Host metrics are unavailable");

        String host = currentHost.get();
        if (isLocalDockerHost(host)) {
            return loadLocalHostResourceUsage(hostMetrics);
        }

        DockerEnv dockerEnv = findCurrentDockerEnv(host);
        if (dockerEnv == null) {
            hostMetrics.put("hostMetricsDebug", "Remote host metrics unavailable: no active Docker environment matched the current host");
            return hostMetrics;
        }

        return remoteHostMetricsCollector.collect(host, dockerEnv);
    }

    private Map<String, Object> loadLocalHostResourceUsage(Map<String, Object> hostMetrics) {
        boolean metricsAvailable = false;
        java.lang.management.OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        // This project targets Java 21 and intentionally uses the JDK/OpenJDK extended OS bean here because the
        // standard management bean does not expose host CPU and memory usage counters with the fidelity we need.
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean extendedOperatingSystemMXBean) {
            double cpuLoad = extendedOperatingSystemMXBean.getCpuLoad();
            if (cpuLoad >= 0) {
                hostMetrics.put("hostCpuUsage", toPercentage(cpuLoad));
                metricsAvailable = true;
            }

            long totalMemory = extendedOperatingSystemMXBean.getTotalMemorySize();
            long freeMemory = extendedOperatingSystemMXBean.getFreeMemorySize();
            if (totalMemory > 0 && freeMemory >= 0) {
                long usedMemory = Math.max(totalMemory - freeMemory, 0);
                hostMetrics.put("hostMemoryUsage", toPercentage((double) usedMemory / totalMemory));
                hostMetrics.put("hostMemoryUsed", ByteUtils.formatBytes(usedMemory));
                hostMetrics.put("hostMemoryTotal", ByteUtils.formatBytes(totalMemory));
                metricsAvailable = true;
            }
        } else {
            log.debug("Extended operating system metrics are not available on the current JVM implementation");
        }

        DiskMetrics diskMetrics = getDiskMetrics();
        long totalDisk = diskMetrics.totalDisk();
        long usableDisk = diskMetrics.usableDisk();
        if (totalDisk > 0) {
            long usedDisk = Math.max(totalDisk - usableDisk, 0);
            hostMetrics.put("hostDiskUsage", toPercentage((double) usedDisk / totalDisk));
            hostMetrics.put("hostDiskUsed", ByteUtils.formatBytes(usedDisk));
            hostMetrics.put("hostDiskTotal", ByteUtils.formatBytes(totalDisk));
            metricsAvailable = true;
        }
        hostMetrics.put("DiskReadTrafficNew", diskMetrics.readTrafficKb());
        hostMetrics.put("WriteTrafficNew", diskMetrics.writeTrafficKb());

        hostMetrics.put("hostMetricsAvailable", metricsAvailable);
        hostMetrics.put("hostMetricsDebug", metricsAvailable
                ? "Local host metrics collected"
                : "Local host metrics unavailable on the current runtime");
        return hostMetrics;
    }

    private DockerEnv findCurrentDockerEnv(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        QueryWrapper<DockerEnv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("host", host).eq("status", 1).last("LIMIT 1");
        return dockerEnvMapper.selectOne(queryWrapper);
    }

    private boolean isLocalDockerHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        if (host.startsWith("unix://") || host.startsWith("npipe://")) {
            return true;
        }

        try {
            URI uri = URI.create(host);
            String hostname = uri.getHost();
            return hostname != null && isLocalAddress(hostname);
        } catch (IllegalArgumentException ex) {
            log.debug("Unable to parse Docker host URI: {}", host, ex);
            return false;
        }
    }

    private boolean isLocalAddress(String hostname) {
        try {
            Set<String> localAddresses = getLocalAddresses();
            for (InetAddress address : InetAddress.getAllByName(hostname)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || localAddresses.contains(address.getHostAddress())) {
                    return true;
                }
            }
        } catch (Exception ex) {
            log.debug("Unable to resolve Docker host address: {}", hostname, ex);
        }
        return false;
    }

    private DiskMetrics getDiskMetrics() {
        synchronized (diskMetricsLock) {
            long now = System.currentTimeMillis();
            if (now - diskMetricsLoadedAt > HOST_METRICS_CACHE_TTL_MILLIS) {
                try {
                    FileStore rootFileStore = Files.getFileStore(Path.of("/"));
                    cachedTotalDisk = rootFileStore.getTotalSpace();
                    cachedUsableDisk = rootFileStore.getUsableSpace();
                } catch (IOException e) {
                    log.warn("Failed to collect root filesystem usage metrics", e);
                    cachedTotalDisk = 0L;
                    cachedUsableDisk = 0L;
                }
                diskMetricsLoadedAt = now;
            }

            double readTrafficKb = 0D;
            double writeTrafficKb = 0D;
            long[] diskIoCounters = readLocalDiskIoCounters();
            long currentReadBytes = diskIoCounters[0];
            long currentWriteBytes = diskIoCounters[1];
            if (previousDiskSampleAt > 0 && now > previousDiskSampleAt
                    && currentReadBytes >= previousDiskReadBytes && currentWriteBytes >= previousDiskWriteBytes) {
                double elapsedSeconds = (now - previousDiskSampleAt) / 1000D;
                if (elapsedSeconds > 0) {
                    readTrafficKb = toTrafficKb(currentReadBytes - previousDiskReadBytes, elapsedSeconds);
                    writeTrafficKb = toTrafficKb(currentWriteBytes - previousDiskWriteBytes, elapsedSeconds);
                }
            }
            previousDiskReadBytes = currentReadBytes;
            previousDiskWriteBytes = currentWriteBytes;
            previousDiskSampleAt = now;

            return new DiskMetrics(cachedTotalDisk, cachedUsableDisk, readTrafficKb, writeTrafficKb);
        }
    }

    private long[] readLocalDiskIoCounters() {
        try {
            String rootBlockDevice = resolveRootBlockDevice();
            if (rootBlockDevice == null || rootBlockDevice.isBlank()) {
                return new long[]{0L, 0L};
            }
            Path statPath = Path.of("/sys/class/block", rootBlockDevice, "stat");
            if (!Files.exists(statPath)) {
                return new long[]{0L, 0L};
            }
            String[] fields = Files.readString(statPath).trim().split("\\s+");
            if (fields.length < 7) {
                return new long[]{0L, 0L};
            }
            return new long[]{parseLong(fields[2]) * 512L, parseLong(fields[6]) * 512L};
        } catch (Exception ex) {
            log.debug("Failed to collect local disk I/O counters", ex);
            return new long[]{0L, 0L};
        }
    }

    private String resolveRootBlockDevice() throws IOException {
        String rootDeviceNumber = resolveRootDeviceNumber();
        if (rootDeviceNumber != null && !rootDeviceNumber.isBlank()) {
            String blockDeviceName = resolveBlockDeviceName(rootDeviceNumber);
            if (blockDeviceName != null && !blockDeviceName.isBlank()) {
                return blockDeviceName;
            }
        }

        Path mountsFilePath = mountsPath();
        if (!Files.isReadable(mountsFilePath)) {
            return null;
        }
        try (Stream<String> mounts = Files.lines(mountsFilePath)) {
            Optional<String> rootMount = mounts
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("\\s+"))
                    .filter(parts -> parts.length >= 2 && "/".equals(parts[1]))
                    .map(parts -> parts[0])
                    .findFirst();
            if (rootMount.isEmpty()) {
                return null;
            }

            String source = rootMount.get();
            if (source.startsWith("/")) {
                try {
                    source = Path.of(source).toRealPath().getFileName().toString();
                } catch (IOException ex) {
                    source = Path.of(source).getFileName().toString();
                }
            }
            return Path.of(source).getFileName().toString();
        }
    }

    private String resolveRootDeviceNumber() throws IOException {
        Path mountInfoPath = mountInfoPath();
        if (!Files.isReadable(mountInfoPath)) {
            // Trigger resolveRootBlockDevice() fallback to /proc/self/mounts when mountinfo
            // is unavailable in the current runtime environment.
            return null;
        }
        try (Stream<String> mountInfoLines = Files.lines(mountInfoPath)) {
            return mountInfoLines
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("\\s+"))
                    .filter(parts -> parts.length >= 5 && "/".equals(parts[4]))
                    .map(parts -> parts[2])
                    .findFirst()
                    .orElse(null);
        }
    }

    Path mountInfoPath() {
        return Path.of("/proc/self/mountinfo");
    }

    Path mountsPath() {
        return Path.of("/proc/self/mounts");
    }

    private String resolveBlockDeviceName(String deviceNumber) {
        try {
            Path devicePath = Path.of("/sys/dev/block", deviceNumber);
            if (!Files.exists(devicePath)) {
                return null;
            }
            return devicePath.toRealPath().getFileName().toString();
        } catch (IOException ex) {
            log.debug("Failed to resolve block device name for {}", deviceNumber, ex);
            return null;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private double toTrafficKb(long bytesDelta, double elapsedSeconds) {
        if (bytesDelta <= 0 || elapsedSeconds <= 0) {
            return 0D;
        }
        return Math.round((bytesDelta / 1024D / elapsedSeconds) * 100D) / 100D;
    }

    private record DiskMetrics(long totalDisk, long usableDisk, double readTrafficKb, double writeTrafficKb) {
    }

    private Set<String> getLocalAddresses() {
        long now = System.currentTimeMillis();
        if (now - localAddressesLoadedAt <= HOST_METRICS_CACHE_TTL_MILLIS && localAddressesLoaded) {
            return cachedLocalAddresses;
        }

        synchronized (localAddressesLock) {
            long refreshedNow = System.currentTimeMillis();
            if (refreshedNow - localAddressesLoadedAt <= HOST_METRICS_CACHE_TTL_MILLIS && localAddressesLoaded) {
                return cachedLocalAddresses;
            }

            Set<String> localAddresses = new HashSet<>();
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        localAddresses.add(inetAddresses.nextElement().getHostAddress());
                    }
                }
            } catch (Exception ex) {
                log.debug("Unable to enumerate local network interfaces", ex);
            }

            cachedLocalAddresses = Set.copyOf(localAddresses);
            localAddressesLoadedAt = refreshedNow;
            localAddressesLoaded = true;
            return cachedLocalAddresses;
        }
    }

    private double toPercentage(double value) {
        return Math.round(value * 10000D) / 100D;
    }

    /**
     * Snapshot Container
     */
    public Map<String, Object> commitContainer(String containerId, String repository, String author) {
        Map<String, Object> result = new HashMap<>();

        try (CommitCmd cmd = getCurrentDockerClient().commitCmd(containerId)) {
            cmd.withRepository(repository);
            // 设置作者
            if (author != null && !author.isEmpty()) {
                cmd.withAuthor(author);
            }
            String imageId = cmd.exec();

            // 成功响应
            result.put("status", "success");
            result.put("imageId", imageId);
            result.put("message", "Container commit successfully");
        } catch (Exception e) {
            // 错误响应
            result.put("status", "failed");
            result.put("message", "Failed to commit container: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取存储卷列表
     */
    public List<InspectVolumeResponse> listVolumed() {
        try (ListVolumesCmd cmd = getCurrentDockerClient().listVolumesCmd()) {
            return cmd.exec().getVolumes();
        }
    }

    /**
     * 搜索存储卷
     */
    public List<InspectVolumeResponse> searchVolumed(VolumeQueryRequest criteria) {
        try (ListVolumesCmd cmd = getCurrentDockerClient().listVolumesCmd()) {

            if (criteria.getName() != null) {
                cmd.withFilter("name", Collections.singleton(criteria.getName()));
            }
            return cmd.exec().getVolumes();
        }
    }

    /**
     * 获取存储卷详情
     */
    public InspectVolumeResponse inspectVolume(String name) {
        try (InspectVolumeCmd cmd = getCurrentDockerClient().inspectVolumeCmd(name)) {
            return cmd.exec();
        }
    }

    /**
     * 检查存储卷是否正在被容器使用
     */
    public boolean isVolumeInUse(String name) {
//        return getCurrentDockerClient().listContainersCmd().withShowAll(true).exec().stream()
//                .anyMatch(container -> container.getMounts().stream()
//                        .anyMatch(mount -> mount.getName() != null && mount.getName().equals(name));

        try (ListContainersCmd cmd = getCurrentDockerClient().listContainersCmd().withShowAll(true)) {
            List<Container> containers = cmd.exec();
            for (Container container : containers) {
                for (ContainerMount mount : container.getMounts()) {
                    if (mount.getName() != null && mount.getName().equals(name)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check volume usage status: {}", e.getMessage());
        }

        return false;
    }

    public List<Map<String, String>> getVolumeContainers(String name) {
        List<Map<String, String>> containers = new ArrayList<>();

        try (ListContainersCmd cmd = getCurrentDockerClient().listContainersCmd().withShowAll(true)) {
            List<Container> containersList = cmd.exec();

            for (Container cr : containersList) {
                String containerId = cr.getId();
                String containerName = cr.getNames()[0].substring(1);
                if (cr.getMounts() == null) continue;

                for (ContainerMount mount : cr.getMounts()) {
                    String volumeName = mount.getName();
                    if (volumeName != null && volumeName.equals(name)) {
                        Map<String, String> containerInfo = new HashMap<>();

                        containerInfo.put("containerId", containerId);
                        containerInfo.put("containerName", containerName);
                        containerInfo.put("volumeName", volumeName);
                        containerInfo.put("destination", mount.getDestination());
                        containerInfo.put("mode", Boolean.TRUE.equals(mount.getRw()) ? "rw" : "ro");
                        containers.add(containerInfo);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Failed to get volume containers: {}", e.getMessage());
        }

        return containers;
    }

    /**
     * 创建存储卷
     */
    public Map<String, Object> createVolume(VolumeCreateRequest criteria) {
        Map<String, Object> result = new HashMap<>();
        try (CreateVolumeCmd cmd = getCurrentDockerClient().createVolumeCmd().withName(criteria.getName()).withDriver(criteria.getDriver())) {
            // 添加标签
            if (criteria.getLabels() != null) {
                cmd.withLabels(stringsToMap(criteria.getLabels()));
            }
            // 添加驱动参数
            if (criteria.getDriverOpts() != null) {
                cmd.withDriverOpts(stringsToMap(criteria.getDriverOpts()));
            }
            // 执行创建卷
            cmd.exec();

            // 成功响应
            result.put("status", "success");
            result.put("message", "Volume create successfully");
        } catch (Exception e) {
            String errorMsg = "Failed to create volume: " + e.getMessage();
            result.put("message", errorMsg);
            result.put("status", "failed");
        }

        return result;
    }

    /**
     * 删除存储卷
     */
    public Map<String, Object> removeVolume(String name) {
        Map<String, Object> result = new HashMap<>();
        try (RemoveVolumeCmd cmd = getCurrentDockerClient().removeVolumeCmd(name)) {
            // 检查存储卷是否正在使用
            if (this.isVolumeInUse(name)) {
                result.put("status", "failed");
                result.put("message", "Volume is in use, please stop the associated containers first");
                return result;
            }

            cmd.exec();
            result.put("status", "success");
            result.put("message", "Volume remove successfully");
        } catch (Exception e) {
            String errorMsg = "Failed to remove volume: " + e.getMessage();
            result.put("message", errorMsg);
            result.put("status", "failed");
        }
        return result;
    }

    /**
     * 获取所有网络
     */
    public List<Network> listNetworks(String name) {
        try (ListNetworksCmd cmd = getCurrentDockerClient().listNetworksCmd()) {

            if (name != null && !name.isEmpty()) {
                cmd.withNameFilter(name);
            }

            return cmd.exec();
        }
    }

    /**
     * 获取网络详情
     */
    public Network inspectNetwork(String networkId) {
        try (InspectNetworkCmd cmd = getCurrentDockerClient().inspectNetworkCmd().withNetworkId(networkId)) {
            return cmd.exec();
        }
    }

    /**
     * 检查网络是否正在被容器使用
     */
    public boolean isNetworkInUse(String networkId) {
        Network detail = this.inspectNetwork(networkId);
        if (detail != null) {
            return detail.getContainers() != null && !detail.getContainers().isEmpty();
        }
        return false;
    }

    /**
     * 创建网络
     */
    public Map<String, Object> createNetwork(NetworkCreateRequest criteria) {
        Map<String, Object> result = new HashMap<>();

        try (CreateNetworkCmd cmd = getCurrentDockerClient().createNetworkCmd().withName(criteria.getName()).withDriver(criteria.getDriver())) {
            // 添加驱动参数
            if (criteria.getDriverOpts() != null) {
                cmd.withOptions(stringsToMap(criteria.getDriverOpts()));
            }

            // 添加标签
            if (criteria.getLabels() != null) {
                cmd.withLabels(stringsToMap(criteria.getLabels()));
            }

            // 开启IPv6
            boolean enableV6 = criteria.getEnableIpv6() != null && criteria.getEnableIpv6();
            cmd.withEnableIpv6(enableV6);

            List<Network.Ipam.Config> ipamConfigs = new ArrayList<>();

            // 添加IPv4配置
            if (criteria.getEnableIpv4() != null && criteria.getEnableIpv4()) {
                Network.Ipam.Config ipamConfig = new Network.Ipam.Config();
                if (criteria.getGateway() != null && !criteria.getGateway().isEmpty()) {
                    ipamConfig.withGateway(criteria.getGateway());
                }
                if (criteria.getIpRange() != null && !criteria.getIpRange().isEmpty()) {
                    ipamConfig.withIpRange(criteria.getIpRange());
                }
                if (criteria.getSubnet() != null && !criteria.getSubnet().isEmpty()) {
                    ipamConfig.withSubnet(criteria.getSubnet());
                }
                ipamConfigs.add(ipamConfig);
            }

            // 添加IPv6配置
            if (criteria.getEnableIpv6() != null && criteria.getEnableIpv6()) {
                Network.Ipam.Config ipamConfig = new Network.Ipam.Config();

                if (criteria.getGatewayV6() != null && !criteria.getGatewayV6().isEmpty()) {
                    ipamConfig.withGateway(criteria.getGatewayV6());
                }
                if (criteria.getIpRangeV6() != null && !criteria.getIpRangeV6().isEmpty()) {
                    ipamConfig.withIpRange(criteria.getIpRangeV6());
                }
                if (criteria.getSubnetV6() != null && !criteria.getSubnetV6().isEmpty()) {
                    ipamConfig.withSubnet(criteria.getSubnetV6());
                }
                ipamConfigs.add(ipamConfig);
            }

            // 设置 IPAM 配置
            if (!ipamConfigs.isEmpty()) {
                Network.Ipam ipam = new Network.Ipam();
                ipam.withConfig(ipamConfigs);
                cmd.withIpam(ipam);
            }

            // 执行创建命令
            cmd.exec();

            // 成功响应
            result.put("status", "success");
            result.put("message", "Network create successfully");

        } catch (Exception e) {
            String errorMsg = "Create network failed!" + e.getMessage();
            result.put("message", errorMsg);
            result.put("status", "failed");
            log.error("Failed to create network: {}", errorMsg, e);
        }
        return result;
    }

    /**
     * 删除网络
     */
    public Map<String, Object> removeNetwork(String name) {
        Map<String, Object> result = new HashMap<>();
        try (RemoveNetworkCmd cmd = getCurrentDockerClient().removeNetworkCmd(name)) {
            // 检查网络是否正在使用
            if (this.isNetworkInUse(name)) {
                result.put("status", "failed");
                result.put("message", "Network is in use, please disconnect it first!");
                return result;
            }

            cmd.exec();
            result.put("status", "success");
            result.put("message", "Network remove successfully");
        } catch (Exception e) {
            String errorMsg = "Failed to remove network: " + e.getMessage();
            result.put("message", errorMsg);
            result.put("status", "failed");
        }
        return result;
    }

    /**
     * 连接容器到网络
     */
    public Map<String, Object> connectNetwork(String networkName, String containerId) {
        Map<String, Object> result = new HashMap<>();
        try (ConnectToNetworkCmd cmd = getCurrentDockerClient().connectToNetworkCmd().withNetworkId(networkName).withContainerId(containerId)) {
            cmd.exec();
            result.put("status", "success");
            result.put("message", "Container connected to network successfully");
        } catch (Exception e) {
            String errorMsg = "Failed to connect container to network: " + e.getMessage();
            result.put("status", "failed");
            result.put("message", errorMsg);
            log.error(errorMsg, e);
        }
        return result;
    }

    /**
     * 断开容器与网络的连接
     */
    public Map<String, Object> disconnectNetwork(String networkName, String containerId) {
        Map<String, Object> result = new HashMap<>();
        try (DisconnectFromNetworkCmd cmd = getCurrentDockerClient().disconnectFromNetworkCmd().withNetworkId(networkName).withContainerId(containerId)) {
            cmd.exec();
            result.put("status", "success");
            result.put("message", "Container disconnected from network successfully");
        } catch (Exception e) {
            String errorMsg = "Failed to disconnect container from network: " + e.getMessage();
            result.put("status", "failed");
            result.put("message", errorMsg);
            log.error(errorMsg, e);
        }
        return result;
    }

    /**
     * 获取容器当前连接的网络
     */
    public Set<String> getContainerNetworks(String containerId) {
        try (InspectContainerCmd cmd = getCurrentDockerClient().inspectContainerCmd(containerId)) {
            InspectContainerResponse containerInfo = cmd.exec();
            return containerInfo.getNetworkSettings().getNetworks().keySet();
        }
    }

    /**
     * 获取镜像列表
     */
    public List<Image> listImages() {
        try (ListImagesCmd cmd = getCurrentDockerClient().listImagesCmd()) {
            return cmd.exec();
        }
    }

    /**
     * 获取镜像详情
     */
    public InspectImageResponse inspectImage(String imageId) {
        try (InspectImageCmd cmd = getCurrentDockerClient().inspectImageCmd(imageId)) {
            return cmd.exec();
        }
    }

    /**
     * 获取镜像历史（通过 Docker HTTP API）
     */
    public List<ImageHistoryItem> getImageHistory(String imageId) {
        // URL-encode imageId to prevent path injection / traversal
        String encodedId = java.net.URLEncoder.encode(imageId, StandardCharsets.UTF_8);

        com.github.dockerjava.transport.DockerHttpClient.Request request =
                com.github.dockerjava.transport.DockerHttpClient.Request.builder()
                        .method(com.github.dockerjava.transport.DockerHttpClient.Request.Method.GET)
                        .path("/images/" + encodedId + "/history")
                        .build();

        try (com.github.dockerjava.transport.DockerHttpClient.Response response =
                     getCurrentDockerHttpClient().execute(request)) {
            if (response.getStatusCode() != 200) {
                log.warn("Failed to get image history for {}: HTTP {}", imageId, response.getStatusCode());
                return new ArrayList<>();
            }

            String json;
            try (InputStream body = response.getBody()) {
                json = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            }

            List<JSONObject> rawList = JSON.parseArray(json, JSONObject.class);
            if (rawList == null) {
                return new ArrayList<>();
            }
            return rawList.stream()
                    .map(item -> {
                        String createdBy = item.getString("CreatedBy");
                        if (createdBy == null) {
                            createdBy = "";
                        }
                        Long sizeBytes = item.getLong("Size");
                        Long createdEpoch = item.getLong("Created");
                        String id = item.getString("Id");
                        String comment = item.getString("Comment");
                        // Docker API returns null for Tags on untagged layers; normalise to empty list
                        List<String> rawTags = item.getList("Tags", String.class);
                        List<String> tags = rawTags != null ? rawTags : new ArrayList<>();

                        String created = createdEpoch != null
                                ? Instant.ofEpochSecond(createdEpoch).toString()
                                : "";
                        String size = sizeBytes != null ? ByteUtils.formatBytes(sizeBytes) : "0 B";

                        return new ImageHistoryItem(id, created, createdBy, size, tags, comment);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get image history for {}", imageId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 推送镜像
     */
    public Map<String, String> pushImage(String repository) {
        Map<String, String> result = new HashMap<>();
        result.put("imageName", repository);

        String taskId = UUID.randomUUID().toString();
        result.put("taskId", taskId);

        // 创建任务
        ImagePushTask task = new ImagePushTask();
        task.setTaskId(taskId);
        task.setImageName(repository);
        task.setStatus("PENDING");
        task.setStartTime(LocalDateTime.now());
        // 添加任务
        imagePushTaskManager.addTask(taskId, task);

        DockerClient client = getCurrentDockerClient();

        CompletableFuture.runAsync(() -> {
            try {
                // 检查是否需要认证
                AuthConfig authConfig = getAuthConfigForImage(repository);

                try (PushImageCmd cmd = client.pushImageCmd(repository)) {
                    // 如果需要认证，设置认证信息
                    if (authConfig != null) {
                        cmd.withAuthConfig(authConfig);
                    }

                    log.info("Pushing image: {}", repository);
                    PushImageResultCallback callback = new PushImageResultCallback() {
                        @Override
                        public void onNext(PushResponseItem item) {
                            log.info("pushing progress: {}", item.getStatus());
                            super.onNext(item);
                        }
                    };

                    task.setStatus("RUNNING");
                    task.setMessage("镜像推送中...");
                    cmd.exec(callback).awaitCompletion();

                    task.setStatus("SUCCESS");
                    task.setMessage("镜像推送成功");
                    task.setEndTime(LocalDateTime.now());
                    log.info("Successfully pushed image: {}", repository);
                }
            } catch (InterruptedException e) {
                // 线程被中断, 更新任务异常状态
                task.setStatus("FAILED");
                task.setMessage("镜像推送失败：线程被中断");

                log.error("Error pushing image: 线程被中断");
                Thread.currentThread().interrupt(); // 保留中断状态
            } catch (Exception e) {
                // 其他异常, 更新任务异常状态
                task.setStatus("FAILED");
                task.setMessage("镜像推送失败：" + e.getMessage());

                log.error("Error pushing image: {}", e.getMessage());
            }
        });

        return result;
    }

    private List<String> extractBaseImageFromDockerfile(String dockerfileContent) {
        List<String> baseImages = new ArrayList<>();

        if (dockerfileContent == null || dockerfileContent.trim().isEmpty()) {
            return baseImages;
        }

        String[] lines = dockerfileContent.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过注释和空行
            if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
                continue;
            }

            // 处理行内注释
            int commentIndex = trimmedLine.indexOf(" #");
            if (commentIndex != -1) {
                trimmedLine = trimmedLine.substring(0, commentIndex).trim();
            }

            if (trimmedLine.toUpperCase().startsWith("FROM ")) {
                String imagePart = trimmedLine.substring(5).trim();
                // 提取基础镜像（处理多阶段构建的别名）
                String[] parts = imagePart.split("\\s+AS\\s+", 2);
                String baseImage = parts[0].trim();

                if (!baseImage.isEmpty()) {
                    baseImages.add(baseImage);
                }
            }
        }
        return baseImages;
    }

    /**
     * 检查镜像是否存在
     */
    private boolean isImageExists(DockerClient client, String imageName) {
        try (InspectImageCmd cmd = client.inspectImageCmd(imageName)) {
            cmd.exec();
            return true;
        } catch (NotFoundException e) {
            log.debug("Image {} does not exist.", imageName);
            return false;
        } catch (Exception e) {
            log.warn("Failed to inspect image: {}", imageName, e);
            throw new RuntimeException("Error occurred while inspecting image", e);
        }
    }

    /**
     * 拉取镜像
     */
    public void doPullImage(DockerClient client, ImageBuildTask task, String imageName) {
        if (isImageExists(client, imageName)) {
            log.info("Image {} already exists. Skipping pull.", imageName);
            return;
        }

        AuthConfig authConfig = getAuthConfigForImage(imageName);

        try (PullImageCmd cmd = client.pullImageCmd(imageName)) {
            if (authConfig != null) {
                cmd.withAuthConfig(authConfig);
            }

            cmd.exec(new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    log.info("Pull progress: {}", item.getStatus());

                    // 使用同步块确保线程安全
                    synchronized (task) {
                        StringBuilder streamBuilder = task.getStreamBuilder();
                        if (streamBuilder == null) {
                            streamBuilder = new StringBuilder();
                            task.setStreamBuilder(streamBuilder);
                        }
                        streamBuilder.append(item.getStatus()).append("\n");
                        // 可选：限制最大长度防止内存溢出
                        if (streamBuilder.length() > 100000) { // 限制100KB
                            streamBuilder.delete(0, streamBuilder.length() - 80000); // 保留后80KB
                        }
                    }

                    super.onNext(item);
                }
            }).awaitCompletion();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("Interrupted during image pulling", e);
        } catch (Exception e) {
            log.error("Failed to pull image: {}", imageName, e);
            throw new RuntimeException("Error occurred while pulling image", e);
        }
    }

    /**
     * 清理虚悬镜像（无tag的中间镜像）
     */
    private long cleanupDanglingImages(DockerClient client) {
        long totalSize = 0L;
        try {
            // 获取所有虚悬镜像（dangling images）
            List<Image> danglingImages;
            try (var listCmd = client.listImagesCmd().withDanglingFilter(true)) {
                danglingImages = listCmd.exec();
            }

            // 删除虚悬镜像
            for (Image image : danglingImages) {
                try {
                    try (var removeCmd = client.removeImageCmd(image.getId())) {
                        removeCmd.exec();
                    }
                    log.info("Removed dangling image: {}", image.getId());

                    long imageSize = image.getSize() != null ? image.getSize() : 0L;
                    totalSize += imageSize;
                } catch (Exception e) {
                    log.warn("Failed to remove dangling image {}: {}", image.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup dangling images: {}", e.getMessage());
        }
        log.info("Total size of removed dangling images: {} bytes", totalSize);
        return totalSize;
    }

    /**
     * 构建镜像
     */
    public Map<String, String> buildImage(
            String dockerfileContent,
            String dockerfilePath,
            String gitUrl,
            String branch,
            String username,
            String password,
            Set<String> tags,
            String buildArgs,
            Boolean pull,
            Boolean noCache,
            String labels,
            String envs,
            MultipartFile[] filesToUpload
    ) {
        log.info("Starting image build process");
        Map<String, String> result = new HashMap<>();
        String taskId = UUID.randomUUID().toString();
        result.put("taskId", taskId);

        // 创建任务
        ImageBuildTask task = new ImageBuildTask();
        task.setTaskId(taskId);
        task.setStatus("PENDING");
        task.setStartTime(LocalDateTime.now());
        // 添加任务
        imageBuildTaskManager.addTask(taskId, task);

        DockerClient client = getCurrentDockerClient();

        // 在主线程中预处理上传的文件
        Map<String, byte[]> uploadedFiles = new HashMap<>();
        if (filesToUpload != null) {
            for (MultipartFile fileToUpload : filesToUpload) {
                try {
                    String fileName = Paths.get(Objects.requireNonNull(fileToUpload.getOriginalFilename())).getFileName().toString();
                    uploadedFiles.put(fileName, fileToUpload.getBytes());
                    log.info("Preloaded file: {} ({} bytes)", fileName, fileToUpload.getSize());
                } catch (IOException e) {
                    log.error("Failed to preload file: {}", fileToUpload.getOriginalFilename(), e);
                    throw new RuntimeException("Failed to process uploaded file: " + fileToUpload.getOriginalFilename(), e);
                }
            }
        }

        CompletableFuture.runAsync(() -> {
            task.setStatus("RUNNING");

            String finalDockerfileContent = dockerfileContent;
            Path gitTempDir = null;

            try {
                // 如果提供了gitUrl，则从Git仓库获取Dockerfile
                if (gitUrl != null && !gitUrl.isEmpty()) {
                    try {
                        gitTempDir = Files.createTempDirectory("git-build-");
                        String gitBranch = (branch != null && !branch.isEmpty()) ? branch : "main";
                        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                            GitUtil.cloneRepositoryWithAuth(gitUrl, username, password, gitBranch, gitTempDir);
                        } else {
                            GitUtil.cloneRepository(gitUrl, gitBranch, gitTempDir);
                        }

                        // 确定 Dockerfile 路径
                        String dockerfileRelativePath = (dockerfilePath != null && !dockerfilePath.isEmpty())
                                ? dockerfilePath
                                : "Dockerfile";

                        Path dockerfileFullPath = gitTempDir.resolve(dockerfileRelativePath);

                        // 读取 Dockerfile 内容
                        finalDockerfileContent = Files.readString(dockerfileFullPath);
                        log.info("Successfully read Dockerfile from git repository: {}", dockerfileFullPath);
                    } catch (Exception e) {
                        log.error("Failed to clone or read from Git repository", e);
                        task.setStatus("FAILED");
                        task.setMessage("Git仓库克隆失败：" + e.getMessage());
                        return;
                    }
                }

                // 预先拉取基础镜像
                List<String> baseImages = extractBaseImageFromDockerfile(finalDockerfileContent);
                if (!baseImages.isEmpty()) {
                    task.setMessage("基础镜像拉取中...");
                    for (String baseImage : baseImages) {
                        try {
                            doPullImage(client, task, baseImage);
                        } catch (Exception e) {
                            log.warn("Failed to pre-pull base image: {}", baseImage, e);
                        }
                    }
                }

                Path tempDir = null;
                try {
                    // 创建临时目录
                    tempDir = Files.createTempDirectory("docker-build-");
                    Path dockerfilePathLocal = tempDir.resolve("Dockerfile");
                    Files.writeString(dockerfilePathLocal, finalDockerfileContent, StandardCharsets.UTF_8);

                    // 存储预处理的文件
                    log.info("Processing {} preloaded files", uploadedFiles.size());
                    for (Map.Entry<String, byte[]> entry : uploadedFiles.entrySet()) {
                        try {
                            String fileName = entry.getKey();
                            byte[] fileContent = entry.getValue();
                            Path targetPath = tempDir.resolve(fileName);
                            log.info("Writing file: {} to {}", fileName, targetPath);
                            Files.write(targetPath, fileContent);
                        } catch (IOException e) {
                            log.error("Failed to write file: {}", entry.getKey(), e);
                            throw new RuntimeException("Failed to write file: " + entry.getKey(), e);
                        }
                    }

                    // 如果是从Git仓库构建，则将Git仓库的内容复制到构建上下文
                    if (gitTempDir != null) {
                        copyGitRepositoryContent(gitTempDir, tempDir);
                    }

                    // 添加调试代码
                    try (Stream<Path> files = Files.list(tempDir)) {
                        log.info("Files in build context: {}", files.collect(Collectors.toList()));
                    }

                    try (BuildImageCmd cmd = client.buildImageCmd()
                            .withRemove(true)
                            .withForcerm(true)
                            .withDockerfile(dockerfilePathLocal.toFile())
                            .withBaseDirectory(tempDir.toFile())  // 设置构建上下文目录
                    ) {
                        // 设置标签
                        cmd.withTags(tags);

                        if (Boolean.TRUE.equals(pull)) {
                            cmd.withPull(true);
                        }
                        if (Boolean.TRUE.equals(noCache)) {
                            cmd.withNoCache(true);
                        }
                        // 安全处理 labels
                        if (labels != null && !labels.isEmpty()) {
                            cmd.withLabels(DockerClientUtil.stringsToMap(labels.split("\n")));
                        }

                        // 添加构建参数
                        if (buildArgs != null && !buildArgs.isEmpty()) {
                            for (String arg : buildArgs.split("\n")) {
                                String[] keyValue = arg.split("=", 2);
                                if (keyValue.length == 2) {
                                    cmd.withBuildArg(keyValue[0], keyValue[1]);
                                } else {
                                    log.warn("Invalid build arg format: {}", arg);
                                }
                            }
                        }

                        // 添加环境变量作为构建参数
                        if (envs != null && !envs.isEmpty()) {
                            for (String env : envs.split("\n")) {
                                String[] keyValue = env.split("=", 2);
                                if (keyValue.length == 2) {
                                    cmd.withBuildArg(keyValue[0], keyValue[1]);
                                } else {
                                    log.warn("Invalid env format: {}", env);
                                }
                            }
                        }

                        BuildImageResultCallback callback = new BuildImageResultCallback() {
                            @Override
                            public void onNext(BuildResponseItem item) {
                                String stream = item.getStream();
                                if (stream != null) {
                                    // 使用同步块确保线程安全
                                    synchronized (task) {
                                        StringBuilder streamBuilder = task.getStreamBuilder();
                                        if (streamBuilder == null) {
                                            streamBuilder = new StringBuilder();
                                            task.setStreamBuilder(streamBuilder);
                                        }
                                        streamBuilder.append(stream);
                                        // 可选：限制最大长度防止内存溢出
                                        if (streamBuilder.length() > 100000) { // 限制100KB
                                            streamBuilder.delete(0, streamBuilder.length() - 80000); // 保留后80KB
                                        }
                                    }
                                    log.info("Building image: {}", stream);
                                }
                                super.onNext(item);
                            }
                        };

                        task.setMessage("镜像正在构建中...");
                        cmd.exec(callback).awaitCompletion();

                        // 镜像构建完成, 更新任务状态
                        task.setStatus("SUCCESS");
                        task.setMessage("镜像构建成功");
                        task.setEndTime(LocalDateTime.now());

                        log.info("Successfully built image: {}", tags);
                    }
                } catch (InterruptedException e) {
                    task.setStatus("FAILED");
                    task.setMessage("镜像构建失败：线程被中断");

                    log.error("Interrupted while building image: {}", e.getMessage());
                    Thread.currentThread().interrupt(); // 恢复中断状态
                } catch (Exception e) {
                    task.setStatus("FAILED");
                    task.setMessage("镜像构建失败：" + e.getMessage());

                    log.error("Error building image: {}", e.getMessage());
                } finally {
                    // 清理临时目录
                    if (tempDir != null) {
                        try {
                            deleteTempDirectory(tempDir);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp directory: {}", tempDir, e);
                        }
                    }
                }
            } finally {
                // 清理Git临时目录
                if (gitTempDir != null) {
                    try {
                        GitUtil.deleteTempDirectory(gitTempDir);
                    } catch (Exception e) {
                        log.warn("Failed to delete git temp directory: {}", gitTempDir, e);
                    }
                }
            }
        });

        return result;
    }

    /**
     * 复制Git仓库内容到构建目录
     */
    private void copyGitRepositoryContent(Path sourceDir, Path targetDir) throws IOException {
        log.info("Copying Git repository content from {} to {}", sourceDir, targetDir);

        try (Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(path -> !path.equals(sourceDir)) // 排除源目录本身
                    .forEach(sourcePath -> {
                        try {
                            Path relativePath = sourceDir.relativize(sourcePath);
                            Path targetPath = targetDir.resolve(relativePath);

                            // 创建目标目录
                            if (Files.isDirectory(sourcePath)) {
                                Files.createDirectories(targetPath);
                            } else {
                                // 复制文件
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to copy file: {}", sourcePath, e);
                        }
                    });
        }

        log.info("Git repository content copied successfully");
    }

    /**
     * 删除临时目录及其内容
     */
    private void deleteTempDirectory(Path tempDir) throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    /**
     * 删除镜像
     */
    public void removeImage(String imageId, Boolean force) {
        try (RemoveImageCmd cmd = getCurrentDockerClient().removeImageCmd(imageId).withForce(force)) {
            cmd.exec();
        }
    }

    /**
     * 回收空间
     */
    public Map<String, Object> pruneCmd(String pruneTypeStr) {
        Map<String, Object> result = new HashMap<>();
        // 参数校验
        if (pruneTypeStr == null || pruneTypeStr.trim().isEmpty()) {
            String errorMsg = "Failed to prune: pruneTypeStr cannot be null or empty";
            result.put("status", "failed");
            result.put("message", errorMsg);
            log.error(errorMsg);
            return result;
        }

        try {
            // 枚举转换安全检查
            PruneType pruneType;
            try {
                pruneType = PruneType.valueOf(pruneTypeStr);
            } catch (IllegalArgumentException e) {
                String errorMsg = "Failed to prune " + pruneTypeStr + ": invalid prune type";
                result.put("status", "failed");
                result.put("message", errorMsg);
                log.error(errorMsg);
                return result;
            }

            long size;
            try (PruneCmd pruneCmd = getCurrentDockerClient().pruneCmd(pruneType)) {
                PruneResponse response = pruneCmd.exec();
                size = response.getSpaceReclaimed() != null ? response.getSpaceReclaimed() : 0L;
            }

            if (pruneTypeStr.equals("IMAGES")) {
                // 获取虚悬镜像列表(指那些没有被任何容器引用的镜像，通常这些镜像的仓库名（镜像名）和标签（TAG）都是<none>)
                long totalSize = cleanupDanglingImages(getCurrentDockerClient());
                size += totalSize;
            }

            result.put("status", "success");
            String sizeStr = ByteUtils.formatBytes(size);
            result.put("message", "prune successfully, " + ", Prune " + sizeStr);
        } catch (Exception e) {
            String errorMsg = "Failed to prune " + pruneTypeStr + ": " + e.getMessage();
            result.put("status", "failed");
            result.put("message", errorMsg);
            log.error(errorMsg, e);
        }

        return result;
    }

    /**
     * 执行命令
     */
    public ContainerExecResponse execCommand(ContainerExecRequest request) {
        ContainerExecResponse response = new ContainerExecResponse();

        try {
            String host = request.getHost();
            setCurrentHost(host);

            String containerId = request.getContainerId();
            String[] command = request.getCommand();

            // 创建执行命令
            try (ExecCreateCmd execCreateCmd = getCurrentDockerClient().execCreateCmd(containerId)) {
                execCreateCmd
                        .withAttachStdin(request.getAttachStdin() != null ? request.getAttachStdin() : true)
                        .withAttachStdout(request.getAttachStdout() != null ? request.getAttachStdout() : true)
                        .withAttachStderr(request.getAttachStderr() != null ? request.getAttachStderr() : true)
                        .withTty(request.getTty() != null ? request.getTty() : true);

                // 设置要执行的命令
                if (command != null && command.length > 0) {
                    execCreateCmd.withCmd(command);
                } else {
                    execCreateCmd.withCmd("/bin/bash");
                }

                ExecCreateCmdResponse execResponse = execCreateCmd.exec();
                String execId = execResponse.getId();
                response.setExecId(execId);

                // 启动执行命令
                try (ExecStartCmd execStartCmd = getCurrentDockerClient().execStartCmd(execId)) {
                    execStartCmd
                            .withDetach(false)
                            .withTty(request.getTty() != null ? request.getTty() : true);

                    StringBuilder output = new StringBuilder();
                    execStartCmd.exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame object) {
                            if (object != null && object.getPayload() != null) {
                                output.append(new String(object.getPayload(), StandardCharsets.UTF_8));
                            }
                        }
                    }).awaitCompletion();

                    response.setOutput(output.toString());
                    response.setStatus("success");
                }
            }
        } catch (Exception e){
            log.error("Failed to execute command in container: {}", request.getContainerId(), e);
            response.setStatus("failed");
            response.setError(e.getMessage());
        }

        return response;
    }

    /**
     * 检查终端是否运行
     */
    public Boolean inspectExecCmd(String execId) {
        try (InspectExecCmd inspectExecCmd =  getCurrentDockerClient().inspectExecCmd(execId)) {
            return inspectExecCmd.exec().isRunning();
        }
    }

    /**
     * 调整终端大小
     */
    public void resizeTerminal(String execId, Integer height, Integer width) {
        try (ResizeExecCmd resizeExecCmd = getCurrentDockerClient().resizeExecCmd(execId)) {
            resizeExecCmd.withSize(height, width);
            resizeExecCmd.exec();
        }
    }

    public static Map<String, String> stringsToMap(String[] strings) {
        Map<String, String> map = new HashMap<>();
        for (String string : strings) {
            if (string.contains("=")) {
                String[] keyValue = string.split("=", 2);
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }

    public static JSONObject toJSON(Object object) {
        try {
            String jsonString = JSONObject.toJSONString(object);
            return (JSONObject) JSON.parse(jsonString);
        } catch (Exception e) {
            // 如果序列化失败，返回一个空的JSONObject
            log.error("Failed to serialize object to JSON", e);
            return new JSONObject();
        }
    }

    /**
     * 从tar输入流中提取文件内容
     */
    private void extractFileFromTar(InputStream tarInputStream, OutputStream outputStream, String containerPath) throws IOException {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(tarInputStream)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = tarInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                return;
            }
        }
        throw new IOException("No file entry found in container archive at path: " + containerPath);
    }

    /**
     * 从容器复制文件到本地
     */
    public byte[] copyFileFromContainer(String containerId, String containerPath) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            copyFileFromContainer(containerId, containerPath, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Unexpected error when copying file from container: {}", containerId, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 从容器中复制文件并将解包后的文件内容直接写入输出流。
     * 适用于下载等需要流式传输的场景，避免将整个文件一次性缓冲到内存中。
     *
     * @param containerId   容器ID
     * @param containerPath 容器内文件路径
     * @param outputStream  目标输出流；会写入解包后的实际文件内容
     */
    public void copyFileFromContainer(String containerId, String containerPath, OutputStream outputStream) throws IOException {
        try (CopyArchiveFromContainerCmd cmd = getCurrentDockerClient().copyArchiveFromContainerCmd(containerId, containerPath)) {
            try (InputStream inputStream = cmd.exec()) {
                extractFileFromTar(inputStream, outputStream, containerPath);
            }
        } catch (IOException e) {
            log.error("I/O error when copying file from container: {}", containerId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error when copying archive from container: {}", containerId, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private InputStream createTarInputStream(MultipartFile file) throws IOException {
        ByteArrayOutputStream tarOutput = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tarOutput)) {
            tarOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            TarArchiveEntry entry = new TarArchiveEntry(file.getOriginalFilename());
            entry.setSize(file.getSize());
            tarOutputStream.putArchiveEntry(entry);

            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    tarOutputStream.write(buffer, 0, bytesRead);
                }
            }

            tarOutputStream.closeArchiveEntry();
            tarOutputStream.finish();
        }

        return new ByteArrayInputStream(tarOutput.toByteArray());
    }

    /**
     * 将文件或目录复制到容器中
     */
    public void copyFileToContainer(String containerId, String containerPath, MultipartFile file) {
        try (InputStream tarInputStream = createTarInputStream(file);
             CopyArchiveToContainerCmd cmd = getCurrentDockerClient().copyArchiveToContainerCmd(containerId)) {
            cmd.withTarInputStream(tarInputStream)
                    .withRemotePath(containerPath)
                    .exec();
        } catch (Exception e) {
            log.error("Failed to copy archive to container: {}", containerId, e);
            throw new RuntimeException("Failed to copy archive to container: " + e.getMessage(), e);
        }
    }

    /**
     * 从tar文件导入镜像
     * @param imageTarInputStream 镜像tar文件的输入流
     * @return 导入结果
     */
    public Map<String, Object> importImage(InputStream imageTarInputStream) {
        Map<String, Object> result = new HashMap<>();

        try (LoadImageCmd cmd = getCurrentDockerClient().loadImageCmd(imageTarInputStream)) {
            cmd.exec();

            result.put("status", "success");
            result.put("message", "镜像导入成功");
        } catch (Exception e) {
            log.error("Failed to import image", e);
            result.put("status", "failed");
            result.put("message", "镜像导入失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 导出镜像到tar文件
     * @param imageName 镜像ID或名称
     * @return 镜像tar文件的字节数组
     */
    public InputStream exportImage(String imageName) {
        try (SaveImageCmd cmd = getCurrentDockerClient().saveImageCmd(imageName)) {

            return cmd.exec();
        } catch (Exception e) {
            log.error("Failed to export image: {}", imageName, e);
            throw new RuntimeException("镜像导出失败: " + e.getMessage(), e);
        }
    }
    // createImageCmd

    /**
     * Export container filesystem as a tar stream.
     * Uses the Docker API directly (GET /containers/{id}/export) since ExportContainerCmd
     * is not available in docker-java 3.6.0.
     * @param containerId Container ID
     * @return Input stream of the container filesystem tar archive. The caller must close
     *         this stream when done; closing it also releases the underlying HTTP response.
     */
    public InputStream exportContainer(String containerId) {
        com.github.dockerjava.transport.DockerHttpClient httpClient = getCurrentDockerHttpClient();
        com.github.dockerjava.transport.DockerHttpClient.Request request =
                new com.github.dockerjava.transport.DockerHttpClient.Request.Builder()
                        .method(com.github.dockerjava.transport.DockerHttpClient.Request.Method.GET)
                        .path("/containers/" + containerId + "/export")
                        .build();
        com.github.dockerjava.transport.DockerHttpClient.Response response = httpClient.execute(request);
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            try {
                response.close();
            } catch (Exception ignore) {
                // ignore close error after status check
            }
            log.error("Docker API returned status {} for container export: {}", statusCode, containerId);
            throw new RuntimeException("Container export failed, HTTP status code: " + statusCode);
        }
        // Wrap the response body so that closing the stream also releases the HTTP response
        InputStream body = response.getBody();
        return new java.io.FilterInputStream(body) {
            @Override
            public void close() throws java.io.IOException {
                try {
                    super.close();
                } finally {
                    try {
                        response.close();
                    } catch (Exception e) {
                        log.warn("Error closing Docker HTTP response for container export: {}", containerId, e);
                    }
                }
            }
        };
    }

    /**
     * 列出容器内指定目录的文件和子目录
     * List files and subdirectories in a specified path inside a container.
     *
     * @param containerId 容器ID
     * @param path        容器内目录路径
     * @return 文件列表，每项包含 name, size, type, permissions
     */
    public List<Map<String, Object>> listContainerFiles(String containerId, String path) {
        List<Map<String, Object>> files = new ArrayList<>();

        // Validate path: must start with '/' and must not contain shell special characters
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (!path.matches("^/[^;|&`$<>\\\\\"'!]*$") && !path.equals("/")) {
            log.warn("Rejected potentially unsafe container path: {}", path);
            throw new IllegalArgumentException("Invalid path: path must be an absolute path without shell special characters");
        }

        try {
            DockerClient client = getCurrentDockerClient();
            // Pass path as a separate argument to ls, NOT interpolated into a shell string,
            // to prevent command injection.
            String[] cmd = {"ls", "-la", path};

            ExecCreateCmdResponse execCreate;
            try (var execCreateCmd = client.execCreateCmd(containerId)
                    .withCmd(cmd)
                    .withUser("root")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)) {
                execCreate = execCreateCmd.exec();
            }

            StringBuilder output = new StringBuilder();
            try (var execStartCmd = client.execStartCmd(execCreate.getId())
                    .withDetach(false)
                    .withTty(false)) {
                execStartCmd.exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        if (frame != null && frame.getPayload() != null) {
                            output.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        }
                    }
                }).awaitCompletion(30, TimeUnit.SECONDS);
            }

            String outputStr = output.toString();
            if (outputStr.isBlank()) {
                return files;
            }

            for (String line : outputStr.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("total ")) {
                    continue;
                }
                Map<String, Object> entry = parseLsLine(line);
                if (entry != null) {
                    files.add(entry);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while listing container files for {}", containerId, e);
        } catch (Exception e) {
            log.error("Error listing container files for {}: {}", containerId, e.getMessage(), e);
        }
        return files;
    }

    /**
     * Parse a single line from `ls -la` output into a map.
     */
    private Map<String, Object> parseLsLine(String line) {
        // Format: permissions links owner group size date time name [-> target]
        String[] parts = line.split("\\s+", 9);
        if (parts.length < 8) {
            return null;
        }
        try {
            String permissions = parts[0];
            String size = parts[4];
            String name;
            String linkTarget = null;

            // Standard ls -la: permissions links owner group size month day time|year name
            // parts: [0]=perm [1]=links [2]=owner [3]=group [4]=size [5]=month [6]=day [7]=time/year [8]=name
            name = parts.length >= 9 ? parts[8] : parts[7];

            // Handle symlinks: "name -> target"
            if (name.contains(" -> ")) {
                String[] nameParts = name.split(" -> ", 2);
                name = nameParts[0].trim();
                linkTarget = nameParts[1].trim();
            } else {
                name = name.trim();
            }

            // Skip . and ..
            if (".".equals(name) || "..".equals(name)) {
                return null;
            }

            String type = permissions.startsWith("d") ? "directory"
                    : permissions.startsWith("l") ? "symlink"
                    : "file";

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("type", type);
            entry.put("permissions", permissions);
            entry.put("size", parseSizeLong(size));
            if (linkTarget != null) {
                entry.put("linkTarget", linkTarget);
            }
            return entry;
        } catch (Exception e) {
            log.debug("Failed to parse ls line: {}", line, e);
            return null;
        }
    }

    private long parseSizeLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 批量操作容器 (start / stop / restart / remove / pause / unpause / kill)
     * Perform a single operation on a list of containers and return per-container results.
     *
     * @param request 包含容器ID列表、主机地址和操作类型
     * @return 每个容器的操作结果列表
     */
    public List<Map<String, Object>> bulkOperateContainers(BulkContainerOperationRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String containerId : request.getContainerIds()) {
            Map<String, Object> containerResult = new HashMap<>();
            containerResult.put("containerId", containerId);

            try {
                Map<String, Object> opResult = switch (request.getOperation().toLowerCase()) {
                    case "start" -> startContainer(containerId);
                    case "stop" -> stopContainer(containerId);
                    case "restart" -> restartContainer(containerId);
                    case "kill" -> killContainer(containerId);
                    case "pause" -> pauseContainer(containerId);
                    case "unpause" -> unpauseContainer(containerId);
                    case "remove" -> {
                        if (request.isForce()) {
                            yield removeContainerForce(containerId);
                        }
                        yield removeContainer(containerId);
                    }
                    default -> Map.of("status", "failed", "message", "Unknown operation: " + request.getOperation());
                };
                containerResult.put("status", opResult.get("status"));
                containerResult.put("message", opResult.get("message"));
            } catch (Exception e) {
                log.error("Bulk operation {} failed for container {}: {}", request.getOperation(), containerId, e.getMessage());
                containerResult.put("status", "failed");
                containerResult.put("message", e.getMessage());
            }
            results.add(containerResult);
        }
        return results;
    }

    /**
     * 更新容器资源限制 (无需重建容器)
     * Update CPU/memory resource limits for a running container without recreation.
     *
     * @param request 包含资源限制参数
     * @return 操作结果
     */
    public Map<String, Object> updateContainerResources(ContainerResourceUpdateRequest request) {
        Map<String, Object> result = new HashMap<>();
        try (UpdateContainerCmd cmd = getCurrentDockerClient().updateContainerCmd(request.getContainerId())) {

            if (request.getCpuShares() != null) {
                cmd.withCpuShares(request.getCpuShares());
            }
            if (request.getCpuQuota() != null) {
                cmd.withCpuQuota(request.getCpuQuota());
            }
            if (request.getCpuPeriod() != null) {
                cmd.withCpuPeriod(request.getCpuPeriod());
            }
            if (request.getMemory() != null) {
                cmd.withMemory(request.getMemory());
            }
            if (request.getMemorySwap() != null) {
                cmd.withMemorySwap(request.getMemorySwap());
            }
            if (request.getMemoryReservation() != null) {
                cmd.withMemoryReservation(request.getMemoryReservation());
            }
            if (request.getBlkioWeight() != null) {
                cmd.withBlkioWeight(request.getBlkioWeight());
            }

            cmd.exec();

            result.put("status", "success");
            result.put("message", "Container resources updated successfully");
        } catch (Exception e) {
            log.error("Failed to update resources for container {}: {}", request.getContainerId(), e.getMessage(), e);
            result.put("status", "failed");
            result.put("message", "Failed to update container resources: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取容器文件系统变更（docker diff）
     * 返回自容器创建以来文件系统中新增、修改或删除的文件列表。
     * kind: 0=修改(Modified), 1=新增(Added), 2=删除(Deleted)
     *
     * @param containerId 容器ID
     * @return 变更列表，每项含 path、kind、kindLabel 字段
     */
    public List<Map<String, Object>> getContainerDiff(String containerId) {
        try (ContainerDiffCmd cmd = getCurrentDockerClient().containerDiffCmd(containerId)) {
            List<ChangeLog> changeLogs = cmd.exec();
            if (changeLogs == null) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<>(changeLogs.size());
            for (ChangeLog changeLog : changeLogs) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("path", changeLog.getPath());
                int kind = changeLog.getKind() != null ? changeLog.getKind() : -1;
                entry.put("kind", kind);
                entry.put("kindLabel", switch (kind) {
                    case 0 -> "Modified";
                    case 1 -> "Added";
                    case 2 -> "Deleted";
                    default -> "Unknown";
                });
                result.add(entry);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get diff for container {}: {}", containerId, e.getMessage(), e);
            throw new RuntimeException("Failed to get container diff: " + e.getMessage(), e);
        }
    }

    /**
     * 读取容器内文件的文本内容
     * 基于 docker cp 机制，将容器内指定文件读取为字符串返回。
     *
     * @param containerId 容器ID
     * @param filePath    容器内文件的绝对路径（必须是文件，不能是目录）
     * @param encoding    字符编码，默认 UTF-8
     * @return 文件的文本内容
     */
    public String readContainerFileContent(String containerId, String filePath, String encoding) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be blank");
        }
        if (!filePath.startsWith("/") || filePath.contains("..") || !filePath.matches("^/[^;|&`$<>\\\\\"'!]*$")) {
            log.warn("Rejected potentially unsafe container file path: {}", filePath);
            throw new IllegalArgumentException("Invalid file path: must be an absolute path without path traversal or shell special characters");
        }

        Charset charset;
        try {
            charset = Charset.forName(encoding != null ? encoding : "UTF-8");
        } catch (Exception e) {
            log.warn("Invalid encoding '{}', falling back to UTF-8", encoding);
            charset = StandardCharsets.UTF_8;
        }

        byte[] fileBytes = copyFileFromContainer(containerId, filePath);
        return new String(fileBytes, charset);
    }

    /**
     * 将文本内容写入容器内的指定文件（新建或覆盖）
     * 通过 docker cp 机制将文本内容打包为 tar 后写入容器。
     *
     * @param containerId 容器ID
     * @param filePath    容器内文件的绝对路径（含文件名）
     * @param content     要写入的文本内容
     * @param encoding    字符编码，默认 UTF-8
     * @return 操作结果
     */
    public Map<String, Object> writeContainerFileContent(String containerId, String filePath, String content, String encoding) {
        Map<String, Object> result = new HashMap<>();

        if (filePath == null || filePath.isBlank()) {
            result.put("status", "failed");
            result.put("message", "File path cannot be blank");
            return result;
        }
        if (!filePath.startsWith("/") || filePath.contains("..") || !filePath.matches("^/[^;|&`$<>\\\\\"'!]*$")) {
            log.warn("Rejected potentially unsafe container file path: {}", filePath);
            result.put("status", "failed");
            result.put("message", "Invalid file path: must be an absolute path without path traversal or shell special characters");
            return result;
        }

        Charset charset;
        try {
            charset = Charset.forName(encoding != null ? encoding : "UTF-8");
        } catch (Exception e) {
            log.warn("Invalid encoding '{}', falling back to UTF-8", encoding);
            charset = StandardCharsets.UTF_8;
        }

        // Extract the directory path and file name from the full file path
        // When lastSlash == 0 (e.g. "/filename.txt"), dirPath correctly becomes "/" and fileName is "filename.txt"
        int lastSlash = filePath.lastIndexOf('/');
        String dirPath = lastSlash > 0 ? filePath.substring(0, lastSlash) : "/";
        String fileName = filePath.substring(lastSlash + 1);

        byte[] contentBytes = content.getBytes(charset);

        try (InputStream tarInputStream = createTextTarInputStream(fileName, contentBytes);
             CopyArchiveToContainerCmd cmd = getCurrentDockerClient().copyArchiveToContainerCmd(containerId)) {
            cmd.withTarInputStream(tarInputStream)
                    .withRemotePath(dirPath)
                    .exec();
            result.put("status", "success");
            result.put("message", "File written successfully");
        } catch (Exception e) {
            log.error("Failed to write file to container {}: {}", containerId, e.getMessage(), e);
            result.put("status", "failed");
            result.put("message", "Failed to write file to container: " + e.getMessage());
        }
        return result;
    }

    /**
     * 搜索 Docker Hub 镜像
     */
    public List<Map<String, Object>> searchImagesOnHub(String term, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (SearchImagesCmd cmd = getCurrentDockerClient().searchImagesCmd(term)) {
            if (limit > 0) {
                cmd.withLimit(limit);
            }
            List<SearchItem> items = cmd.exec();
            for (SearchItem item : items) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", item.getName());
                entry.put("description", item.getDescription());
                entry.put("starCount", item.getStarCount());
                entry.put("isOfficial", item.isOfficial());
                entry.put("isTrusted", item.isTrusted());
                results.add(entry);
            }
        } catch (Exception e) {
            log.error("Failed to search Docker Hub images for term '{}': {}", term, e.getMessage(), e);
        }
        return results;
    }

    /**
     * 将字节内容打包为 tar 输入流（用于写入容器文件）
     */
    private InputStream createTextTarInputStream(String fileName, byte[] content) throws IOException {
        ByteArrayOutputStream tarOutput = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tarOutput)) {
            tarOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            TarArchiveEntry entry = new TarArchiveEntry(fileName);
            entry.setSize(content.length);
            tarOutputStream.putArchiveEntry(entry);
            tarOutputStream.write(content);
            tarOutputStream.closeArchiveEntry();
            tarOutputStream.finish();
        }
        return new ByteArrayInputStream(tarOutput.toByteArray());
    }

    /**
     * 确保卷操作所需的辅助镜像可用，如本地不存在则同步拉取
     */
    private void ensureVolumeHelperImage(DockerClient client) {
        if (isImageExists(client, VOLUME_HELPER_IMAGE)) {
            return;
        }
        log.info("Helper image {} not found locally, pulling...", VOLUME_HELPER_IMAGE);
        try (PullImageCmd cmd = client.pullImageCmd(VOLUME_HELPER_IMAGE)) {
            cmd.exec(new PullImageResultCallback()).awaitCompletion();
            log.info("Helper image {} pulled successfully", VOLUME_HELPER_IMAGE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while pulling helper image: " + VOLUME_HELPER_IMAGE, e);
        } catch (Exception e) {
            log.error("Failed to pull helper image {}: {}", VOLUME_HELPER_IMAGE, e.getMessage());
            throw new RuntimeException("Failed to pull helper image: " + VOLUME_HELPER_IMAGE, e);
        }
    }

    /**
     * 备份存储卷：通过临时容器将卷内容导出为 TAR 流。
     * <p>
     * 创建一个挂载了指定卷的临时 busybox 容器，然后将卷内容（或指定子路径）以 TAR 格式流式返回。
     * <strong>调用方必须关闭返回的 {@link InputStream}</strong>，关闭时会同步清理临时容器和底层 Docker 连接。
     *
     * @param volumeName 需要备份的 Docker 卷名称
     * @param path       卷内需要备份的子路径（相对于卷根目录）；传 {@code null} 或空字符串时备份整个卷根目录
     * @return 包含卷内容的 TAR 格式输入流，调用方负责关闭
     */
    public InputStream backupVolume(String volumeName, String path) {
        DockerClient client = getCurrentDockerClient();
        String backupPath = (path == null || path.isBlank()) ? "/" : path;
        String containerName = VOLUME_BACKUP_CONTAINER_PREFIX + volumeName + "-" + System.currentTimeMillis();

        ensureVolumeHelperImage(client);

        Volume vol = new Volume(VOLUME_BACKUP_MOUNT_PATH);
        Bind bind = new Bind(volumeName, vol);

        String containerId;
        try (CreateContainerCmd cmd = client.createContainerCmd(VOLUME_HELPER_IMAGE)
                .withName(containerName)
                .withCmd("true")
                .withHostConfig(HostConfig.newHostConfig().withBinds(bind))) {
            containerId = cmd.exec().getId();
        }

        // Resolve the copy path inside the container
        String normalizedBackupPath = backupPath.startsWith("/") ? backupPath : "/" + backupPath;
        String copyPath = "/".equals(normalizedBackupPath) ? VOLUME_BACKUP_MOUNT_PATH : VOLUME_BACKUP_MOUNT_PATH + normalizedBackupPath;

        // Do NOT close copyCmd here: exec() returns a live HTTP-response stream and closing
        // the command object closes the underlying connection, producing an empty stream.
        // Both copyCmd and the container are cleaned up when the caller closes the returned stream.
        CopyArchiveFromContainerCmd copyCmd = client.copyArchiveFromContainerCmd(containerId, copyPath);
        InputStream tarStream = copyCmd.exec();

        final String finalContainerId = containerId;
        return new FilterInputStream(tarStream) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    try {
                        copyCmd.close();
                    } catch (Exception ignored) {
                        log.warn("Failed to close copyCmd for backup container: {}", finalContainerId);
                    }
                    try {
                        client.removeContainerCmd(finalContainerId).withForce(true).exec();
                    } catch (Exception ignored) {
                        log.warn("Failed to remove temporary backup container: {}", finalContainerId);
                    }
                }
            }
        };
    }

    /**
     * 克隆存储卷：将源卷的数据复制到目标卷
     */
    public Map<String, Object> cloneVolume(String sourceName, String targetName, String driver) {
        Map<String, Object> result = new HashMap<>();
        DockerClient client = getCurrentDockerClient();

        // Create the target volume
        try (CreateVolumeCmd createCmd = client.createVolumeCmd()
                .withName(targetName)
                .withDriver(driver != null && !driver.isBlank() ? driver : "local")) {
            createCmd.exec();
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("message", "Failed to create target volume: " + e.getMessage());
            return result;
        }

        String containerName = VOLUME_CLONE_CONTAINER_PREFIX + System.currentTimeMillis();
        Volume srcVol = new Volume(VOLUME_CLONE_SRC_MOUNT);
        Volume dstVol = new Volume(VOLUME_CLONE_DST_MOUNT);
        Bind srcBind = new Bind(sourceName, srcVol);
        Bind dstBind = new Bind(targetName, dstVol);

        ensureVolumeHelperImage(client);

        String containerId = null;
        try {
            try (CreateContainerCmd cmd = client.createContainerCmd(VOLUME_HELPER_IMAGE)
                    .withName(containerName)
                    .withCmd("sh", "-c", "cp -a " + VOLUME_CLONE_SRC_MOUNT + "/. " + VOLUME_CLONE_DST_MOUNT + "/")
                    .withHostConfig(HostConfig.newHostConfig().withBinds(srcBind, dstBind))) {
                containerId = cmd.exec().getId();
            }

            try (StartContainerCmd cmd = client.startContainerCmd(containerId)) {
                cmd.exec();
            }

            try (WaitContainerCmd waitCmd = client.waitContainerCmd(containerId)) {
                waitCmd.start().awaitCompletion(VOLUME_CLONE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }

            result.put("status", "success");
            result.put("message", "Volume cloned successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("message", "Failed to clone volume: " + e.getMessage());
        } finally {
            if (containerId != null) {
                try {
                    client.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {
                    log.warn("Failed to remove temporary clone container: {}", containerId);
                }
            }
        }

        return result;
    }

    /**
     * 批量删除镜像，返回每个镜像的删除结果
     */
    public List<Map<String, Object>> bulkRemoveImages(List<String> imageIds, Boolean force) {
        List<Map<String, Object>> results = new ArrayList<>();
        boolean forceRemove = Boolean.TRUE.equals(force);

        for (String imageId : imageIds) {
            Map<String, Object> itemResult = new HashMap<>();
            itemResult.put("imageId", imageId);
            try {
                this.removeImage(imageId, forceRemove);
                itemResult.put("status", "success");
                itemResult.put("message", "Image removed successfully");
            } catch (Exception e) {
                itemResult.put("status", "failed");
                itemResult.put("message", "Failed to remove image: " + e.getMessage());
            }
            results.add(itemResult);
        }

        return results;
    }

    /**
     * 获取镜像磁盘使用情况
     */
    public List<ImageUsageItem> getImageDiskUsage() {
        List<Image> images = listImages();
        List<Container> containers = listContainers();
        Set<String> usedImageIds = containers.stream()
                .map(Container::getImageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return images.stream()
                .filter(img -> img.getRepoTags() != null
                        && Arrays.stream(img.getRepoTags()).noneMatch("<none>:<none>"::equals))
                .map(img -> {
                    ImageUsageItem item = new ImageUsageItem();
                    String fullId = img.getId() != null ? img.getId() : "";
                    String shortId = fullId.startsWith("sha256:") && fullId.length() >= 19
                            ? fullId.substring(7, 19)
                            : (fullId.length() >= 12 ? fullId.substring(0, 12) : fullId);
                    item.setId(shortId);
                    item.setFullId(fullId);
                    item.setRepoTags(img.getRepoTags() != null
                            ? Arrays.asList(img.getRepoTags()) : Collections.emptyList());
                    long sz = img.getSize() != null ? img.getSize() : 0L;
                    long vsz = img.getVirtualSize() != null ? img.getVirtualSize() : 0L;
                    item.setSize(sz);
                    item.setSizeHuman(ByteUtils.formatBytes(sz));
                    item.setVirtualSize(vsz);
                    item.setVirtualSizeHuman(ByteUtils.formatBytes(vsz));
                    item.setIsUsed(usedImageIds.contains(img.getId()));
                    return item;
                })
                .sorted(Comparator.comparingLong(ImageUsageItem::getVirtualSize).reversed())
                .toList();
    }
}
