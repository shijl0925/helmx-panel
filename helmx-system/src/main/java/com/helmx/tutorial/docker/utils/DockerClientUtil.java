package com.helmx.tutorial.docker.utils;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.helmx.tutorial.docker.dto.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import com.alibaba.fastjson2.JSONObject;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DockerClientUtil {

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

    // 设置当前操作的服务器
    public void setCurrentHost(String host) {
        currentHost.set(host);
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

    // 获取当前Docker连接状态
    public boolean isConnectionHealthy() {
        String host = currentHost.get();
        if (host == null) {
            return false;
        }

        try {
            DockerClient client = getCurrentDockerClient();
            client.pingCmd().exec();
            return true;
        } catch (Exception e) {
//            log.debug("Connection health check failed for host: {}", host, e);
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
     * 启动容器
     */
    public Map<String, Object> startContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();

        try {
            try (StartContainerCmd cmd = getCurrentDockerClient().startContainerCmd(containerId)) {
                cmd.exec();
            }
            // 成功响应
            result.put("status", "success");
            result.put("message", "Container start successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to start container: " + e.getMessage();
            result.put("message", errorMsg);
        }

        return result;
    }

    /**
     * 停止容器
     */
    public Map<String, Object> stopContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            try (StopContainerCmd cmd = getCurrentDockerClient().stopContainerCmd(containerId)) {
                cmd.exec();
            }

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container stop successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to stop container: " + e.getMessage();
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 重启容器
     */
    public Map<String, Object> restartContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            try (RestartContainerCmd cmd = getCurrentDockerClient().restartContainerCmd(containerId)) {
                cmd.exec();
            }

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container restart successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to restart container: " + e.getMessage();
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 删除容器
     */
    public Map<String, Object> removeContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            try (RemoveContainerCmd cmd = getCurrentDockerClient().removeContainerCmd(containerId)) {
                cmd.exec();
            }

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container remove successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to remove container: " + e.getMessage();
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 杀死容器
     */
    public Map<String, Object> killContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            try (KillContainerCmd cmd = getCurrentDockerClient().killContainerCmd(containerId)) {
                cmd.exec();
            }
            // 成功响应
            result.put("status", "success");
            result.put("message", "Container kill successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to kill container: " + e.getMessage();
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 暂停容器
     */
    public Map<String, Object>  pauseContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            try (PauseContainerCmd cmd = getCurrentDockerClient().pauseContainerCmd(containerId)) {
                cmd.exec();
            }

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container pause successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to pause container: " + e.getMessage();
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 恢复容器
     */
    public Map<String, Object> unpauseContainer(String containerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            try (UnpauseContainerCmd cmd = getCurrentDockerClient().unpauseContainerCmd(containerId)) {
                cmd.exec();
            }

            // 成功响应
            result.put("status", "success");
            result.put("message", "Container unpause successfully");
        } catch (Exception e) {
            result.put("status", "failed");
            String errorMsg = "Failed to unpause container: " + e.getMessage();
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 获取容器日志
     */
    public String getContainerLogs(String containerId, int tailNum) {
        StringBuilder logs = new StringBuilder();

        try (LogContainerCmd cmd = getCurrentDockerClient().logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)) {

            if (tailNum != 0) {
                cmd.withTail(tailNum);
            }

            cmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    logs.append(new String(item.getPayload())).append("\n");
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 保留中断状态
            throw new RuntimeException("获取容器日志被中断", e);
        } catch (Exception e) {
            throw new RuntimeException("获取容器日志失败", e);
        }

        return logs.toString();
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
            Registry registry = registryMapper.selectOne(queryWrapper);

            if (registry != null && registry.getAuth() != null && registry.getAuth()) {
                return new AuthConfig()
                        .withRegistryAddress(registry.getUrl())
                        .withUsername(registry.getUsername())
                        .withPassword(registry.getPassword());
            }
        } catch (Exception e) {
            log.warn("Failed to get auth config for image: {}", imageName, e);
        }
        return null;
    }

    /**
     * 拉取镜像（如果不存在）
     */
    public Map<String, String> pullImageIfNotExists(String imageName) throws InterruptedException {
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
        String[] parts = tagImageName.split(":");
        String imageNameWithRepository = parts[0];
        String tag = parts.length > 1 ? parts[1] : "latest";
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

        // 设置输入
        if (criteria.getStdinOpen() != null) {
            createContainerCmd.withStdinOpen(criteria.getStdinOpen());
        }

        if (criteria.getTty() != null) {
            createContainerCmd.withTty(criteria.getTty());
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
        if (criteria.getNetworkMode() != null && !networkMode.isEmpty()) {
            hostConfig.withNetworkMode(networkMode);
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
            CreateContainerCmd createContainerCmd = getCurrentDockerClient().createContainerCmd(imageName);

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
            CreateContainerCmd createContainerCmd = getCurrentDockerClient().createContainerCmd(newImageName)
                    .withName(newContainerName);
            log.info("Creating container: {} with image: {}", newContainerName, newImageName);

            // 设置容器配置
            configureContainer(createContainerCmd, criteria);

            // 执行创建容器命令
            CreateContainerResponse newContainer = createContainerCmd.exec();
            String newContainerId = newContainer.getId();
            log.info("Created new container with ID: {}", newContainerId);

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
        List<Image> images;
        try (ListImagesCmd cmd = getCurrentDockerClient().listImagesCmd().withShowAll(true)) {
            images = cmd.exec();
        }

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

            result.put("created", created);
            result.put("running", running);
            result.put("paused", paused);
            result.put("stopped", stopped);
            result.put("exited", exited);
            result.put("restarting", restarting);
            result.put("removing", removing);
            result.put("dead", dead);
        }

        return result;
    }

    /**
     * Snapshot Container
     */
    public Map<String, Object> commitContainer(String containerId, String repository) {
        Map<String, Object> result = new HashMap<>();

        try (CommitCmd cmd = getCurrentDockerClient().commitCmd(containerId)) {
            cmd.withRepository(repository);
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

            // 添加IPv4
            if (criteria.getEnableIpv4() != null && criteria.getEnableIpv4()) {
                Network.Ipam.Config ipamConfig = new Network.Ipam.Config();
                ipamConfig.withGateway(criteria.getGateway());
                ipamConfig.withIpRange(criteria.getIpRange());
                ipamConfig.withSubnet(criteria.getSubnet());
                ipamConfigs.add(ipamConfig);
            }

            // 添加IPv6
            if (criteria.getEnableIpv6() != null && criteria.getEnableIpv6()) {
                Network.Ipam.Config ipamConfig = new Network.Ipam.Config();
                ipamConfig.withGateway(criteria.getGatewayV6());
                ipamConfig.withIpRange(criteria.getIpRangeV6());
                ipamConfig.withSubnet(criteria.getSubnetV6());
                ipamConfigs.add(ipamConfig);
            }

            // 设置 IPAM 配置
            Network.Ipam ipam = new Network.Ipam();
            ipam.withConfig(ipamConfigs);
            cmd.withIpam(ipam);

            // 执行创建命令
            cmd.exec();

            // 成功响应
            result.put("status", "success");
            result.put("message", "Network create successfully");

        } catch (Exception e) {
            String errorMsg = "Create network failed!" + e.getMessage();
            result.put("message", errorMsg);
            result.put("status", "failed");
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
        try (ListImagesCmd cmd = getCurrentDockerClient().listImagesCmd().withShowAll(true)) {
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
     * 获取镜像历史
     */
    public List<Map<String, String>> getImageHistory(String imageId) {
        try {
            Process process = Runtime.getRuntime().exec("docker history --no-trunc --format {{.CreatedSince}}|||{{.CreatedBy}}|||{{.Size}} " + imageId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            List<Map<String, String>> history = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|\\|\\|", 3);
                if (parts.length == 3) {
                    Map<String, String> historyItem = new HashMap<>();
                    historyItem.put("created", parts[0].trim());
                    historyItem.put("layer", parts[1].trim());
                    historyItem.put("size", parts[2].trim());
                    history.add(historyItem);
                }
            }

            return history;
        } catch (Exception e) {
            log.warn("Failed to get image history via command line", e);
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

    /**
     * 构建镜像
     */
    public Map<String, String> buildImage(String dockerfileContent, Set<String> tags, String buildArgs, Boolean pull, Boolean noCache, String labels, String envs, MultipartFile[] filesToUpload) {
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

        CompletableFuture.runAsync(() -> {
            Path tempDir = null;
            try {
                // 创建临时目录和 Dockerfile
                tempDir = Files.createTempDirectory("docker-build-");
                Path dockerfilePath = tempDir.resolve("Dockerfile");
                Files.writeString(dockerfilePath, dockerfileContent, StandardCharsets.UTF_8);

                // 存储文件
                if (filesToUpload != null && filesToUpload.length > 0) {
                    for (MultipartFile fileToUpload : filesToUpload) {
                        String safeFileName = Paths.get(Objects.requireNonNull(fileToUpload.getOriginalFilename())).getFileName().toString();
                        Files.copy(fileToUpload.getInputStream(), tempDir.resolve(safeFileName), StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                try (BuildImageCmd cmd = client.buildImageCmd().withDockerfile(dockerfilePath.toFile()).withTags(tags)) {
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

                    task.setStatus("RUNNING");
                    task.setMessage("镜像构建中...");
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
        });

        return result;
    }

    /**
     * 删除临时目录及其内容
     */
    private void deleteTempDirectory(Path tempDir) throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
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

            PruneCmd pruneCmd = getCurrentDockerClient().pruneCmd(pruneType);
            PruneResponse response = pruneCmd.exec();
            long size = response.getSpaceReclaimed() != null ? response.getSpaceReclaimed() : 0L;

            if (pruneTypeStr.equals("IMAGES")) {
                // 获取虚悬镜像列表(指那些没有被任何容器引用的镜像，通常这些镜像的仓库名（镜像名）和标签（TAG）都是<none>)
                List<Image> images = getCurrentDockerClient().listImagesCmd().withDanglingFilter(true).exec();
                log.info("Dangling image: {}", images);
                long totalSize = 0L;
                for (Image image : images) {
                    try {
                        long imageSize = image.getSize() != null ? image.getSize() : 0L;
                        totalSize += imageSize;
                        getCurrentDockerClient().removeImageCmd(image.getId()).exec();
                    } catch (Exception e) {
                        log.error("Failed to remove image: {}", image.getId(), e);
                    }
                }
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
                                output.append(new String(object.getPayload()));
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
    private byte[] extractFileFromTar(InputStream tarInputStream) throws IOException {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(tarInputStream)) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                // 跳过目录条目
                if (entry.isDirectory()) {
                    continue;
                }

                // 读取文件内容
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = tarInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return outputStream.toByteArray();
            }
        }

        // 如果没有找到文件条目，返回空字节数组
        return new byte[0];
    }

    /**
     * 从容器复制文件到本地
     */
    public byte[] copyFileFromContainer(String containerId, String containerPath) {
        try (CopyArchiveFromContainerCmd cmd = getCurrentDockerClient().copyArchiveFromContainerCmd(containerId, containerPath)) {
            InputStream inputStream = cmd.exec();
            // 解压tar文件获取实际文件内容
            return extractFileFromTar(inputStream);
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

            InputStream inputStream = file.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                tarOutputStream.write(buffer, 0, bytesRead);
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

    // createImageCmd
    // saveImageCmd
    // buildImageCmd
}
