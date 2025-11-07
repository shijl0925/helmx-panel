package com.helmx.tutorial.docker.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DockerCompose {

    @Autowired
    private ComposeBuildTaskManager composeBuildTaskManager;

    @Value("${docker.compose.save.path:docker-compose/}")
    private String composeDeployPath;

    /**
     * 使用 docker-compose 文件构建和部署服务栈，并在部署过程中获取日志
     */
    public Map<String, String> deployCompose(Long stackId, String content) {
        log.info("Starting build compose process for stack: {}", stackId);
        Map<String, String> result = new HashMap<>();
        String taskId = UUID.randomUUID().toString();
        result.put("taskId", taskId);

        // 创建任务
        ComposeBuildTask task = new ComposeBuildTask();
        task.setTaskId(taskId);
        task.setStatus("PENDING");
        task.setStartTime(LocalDateTime.now());

        // 添加任务
        composeBuildTaskManager.addTask(taskId, task);

        CompletableFuture.runAsync(() -> {
            task.setStatus("RUNNING");
            Process deployProcess = null;

            try {
                // 检查docker-compose 插件是否可用
                if (!isDockerComposeAvailable()) {
                    throw new RuntimeException("docker-compose or docker compose command is not available");
                }

                // 准备 docker-compose 文件
                PathInfo pathInfo = prepareComposeFile(stackId, content);
                Path stackDirPath = pathInfo.stackDirPath;
                Path composeFile = pathInfo.composeFile;

                // 先清理已存在的服务
                task.setMessage("清理已有服务...");
                try {
                    cleanupExistingStack(composeFile, stackDirPath);
                } catch (Exception e) {
                    log.warn("Failed to cleanup existing stack, continuing with deployment", e);
                    task.setMessage("清理服务失败，继续部署...");
                }

                task.setMessage("开始部署...");

                // 构建 docker-compose up 命令
                List<String> deployCommand = buildComposeCommand("-f", composeFile.toString(), "up", "-d");
                log.info("Deploying compose, command: {}", deployCommand);

                ProcessBuilder deployProcessBuilder = new ProcessBuilder(deployCommand);

                deployProcessBuilder.directory(stackDirPath.toFile());

                // 启动部署进程
                deployProcess = deployProcessBuilder.start();
                task.setMessage("Starting stack deployment...");

                // 等待命令执行完成（设置超时时间）
                boolean finished = deployProcess.waitFor(300, TimeUnit.SECONDS); // 5分钟超时

                if (!finished) {
                    deployProcess.destroyForcibly();
                    throw new RuntimeException("Deployment timeout after 5 minutes");
                }

                int exitCode = deployProcess.waitFor();

                if (exitCode != 0) {
                    // 读取错误输出
                    String errorOutput = readErrorStream(deployProcess);

                    task.setStatus("FAILED");
                    task.setMessage("Failed to deploy stack. Exit code: " + exitCode + ". Error: " + errorOutput);
                    log.error("Failed to deploy stack. Exit code: {}. Error: {}", exitCode, errorOutput);
                } else {
                    task.setStatus("SUCCESS");
                    task.setMessage("Stack deployed successfully.");
                    log.info("Stack deployed successfully.");

                    task.setEndTime(LocalDateTime.now());

                    List<String> services = getRunningServices(composeFile, stackDirPath);
                    log.info("Running services: {}", services);
                }
            } catch (Exception e) {
                task.setStatus("FAILED");
                task.setMessage("Failed to deploy stack: " + e.getMessage());
                log.error("Failed to deploy stack: {}", e.getMessage(), e);
            } finally {
                // 清理资源
                closeQuietly(deployProcess);
            }
        });

        return result;
    }

    private void cleanupExistingStack(Path composeFile, Path stackDirPath) throws IOException, InterruptedException {
        List<String> command = buildComposeCommand("-f", composeFile.toString(), "down", "--remove-orphans");
        log.info("Cleaning up existing stack, command: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(stackDirPath.toFile());
        Process process = processBuilder.start();

        boolean finished = process.waitFor(120, TimeUnit.SECONDS); // 2分钟超时
        if (!finished) {
            process.destroyForcibly();
            log.warn("docker-compose down command timeout during cleanup");
        } else {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorOutput = readErrorStream(process);
                log.warn("Failed to cleanup existing stack. Exit code: {}. Error: {}", exitCode, errorOutput);
            } else {
                log.info("Existing stack cleaned up successfully");
            }
        }
        closeQuietly(process);
    }

    /**
     * 获取 docker-compose 服务的日志
     * @param content docker-compose 文件内容
     * @param follow 是否持续跟踪日志
     * @param tail 显示最后几行日志 (可选)
     * @return 日志内容或异步日志流
     */
    public String getComposeLogs(Long stackId, String content, boolean follow, Integer tail) {
        Process process = null;

        try {
            // 检查 docker-compose 命令是否可用
            if (!isDockerComposeAvailable()) {
                throw new RuntimeException("docker-compose or docker compose command is not available");
            }

            // 准备 docker-compose 文件
            PathInfo pathInfo = prepareComposeFile(stackId, content);
            Path stackDirPath = pathInfo.stackDirPath;
            Path composeFile = pathInfo.composeFile;

            // 构建 docker-compose logs 命令
            List<String> command = buildComposeCommand("-f", composeFile.toString(), "logs");
            if (follow) {
                command.add("-f");
            }
            if (tail != null && tail > 0) {
                command.add("--tail");
                command.add(tail.toString());
            }
            log.info("Getting compose logs, command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(stackDirPath.toFile());

            process = processBuilder.start();

            // 如果需要持续跟踪日志，可以返回 InputStream 或使用回调处理
            if (follow) {
                // 对于持续跟踪日志，可以考虑返回 InputStream 或使用异步回调
                // 这里简单地读取一部分日志作为示例
                return readLogStream(process, 10000); // 读取最多10秒的日志
            } else {
                // 读取全部日志并返回
                return readLogStream(process, 0); // 读取所有日志
            }
        } catch (Exception e) {
            log.error("Failed to get compose logs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get compose logs: " + e.getMessage(), e);
        } finally {
            // 清理资源
            closeQuietly(process);
        }
    }

    /**
     * 读取进程日志流
     * @param process 进程对象
     * @param timeoutMillis 超时时间（毫秒），0表示无超时
     * @return 日志内容
     */
    private String readLogStream(Process process, int timeoutMillis) throws IOException {
        StringBuilder logs = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            if (timeoutMillis > 0) {
                // 设置超时读取
                long startTime = System.currentTimeMillis();
                String line;

                while ((line = reader.readLine()) != null &&
                        (System.currentTimeMillis() - startTime) < timeoutMillis) {
                    logs.append(line).append("\n");
                }
            } else {
                // 读取所有日志
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                }
            }
        }

        return logs.toString();
    }

    // 内部工具类，用于封装路径信息
    private record PathInfo(Path stackDirPath, Path composeFile) {}

    // docker-compose 文件的准备
    private PathInfo prepareComposeFile(Long stackId, String content) throws IOException {
        // 创建目录存储 docker-compose 内容
        Path basePath = Paths.get(composeDeployPath).toAbsolutePath().normalize();

        log.debug("Preparing compose file for stackId: {}, basePath: {}", stackId, basePath);
        Path stackDirPath = basePath.resolve(stackId.toString()).normalize();
        Files.createDirectories(stackDirPath);
        Path composeFile = stackDirPath.resolve("docker-compose.yaml");

        // 将 content 写入文件（如果文件已存在且内容相同则跳过）
        if (!Files.exists(composeFile) || !content.equals(Files.readString(composeFile))) {
            Files.write(composeFile, content.getBytes(StandardCharsets.UTF_8));
        }

        return new PathInfo(stackDirPath, composeFile);
    }


    // 获取成功部署服务
    public List<String> getRunningServices(Path composeFile, Path stackDirPath) {
        List<String> runningServices = new ArrayList<>();
        try {
            List<String> checkCommand = buildComposeCommand("-f", composeFile.toString(), "ps");
            ProcessBuilder checkProcessBuilder = new ProcessBuilder(checkCommand);
            checkProcessBuilder.directory(stackDirPath.toFile());
            Process checkProcess = checkProcessBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(checkProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = checkProcess.waitFor();
            // exitCode为0表示命令执行成功
            // 如果没有运行中的服务，输出应该只包含标题行或为空
            if (exitCode == 0) {
                String outputStr = output.toString().trim();
                if (!outputStr.isEmpty()) {
                    String[] lines = outputStr.split("\n");
                    for (int i = 1; i < lines.length; i++) {
                        String[] parts = lines[i].trim().split("\\s+");
                        if (parts.length >= 2) {
                            runningServices.add(parts[0]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get compose services: {}", e.getMessage(), e);
        }

        return runningServices;
    }

    /**
     * 构建 docker-compose 命令
     */
    private List<String> buildComposeCommand(String... args) {
        List<String> command = new ArrayList<>();

        // 尝试使用新的 docker compose 命令
        if (isDockerComposePluginAvailable()) {
            command.add("docker");
            command.add("compose");
        } else {
            command.add("docker-compose");
        }

        command.addAll(Arrays.asList(args));
        return command;
    }

    /**
     * 检查 docker-compose 插件 或者 docker compose 是否可用
     */
    private boolean isDockerComposeAvailable() {
        // 优先检查新的 docker compose 命令
        if (isDockerComposePluginAvailable()) {
            return true;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("docker-compose", "--version");
            Process process = processBuilder.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            log.debug("docker-compose command not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 docker-compose 插件是否可用
     */
    private boolean isDockerComposePluginAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("docker", "compose", "--version");
            Process process = processBuilder.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安静地关闭进程的所有流
     */
    private void closeQuietly(Process process) {
        if (process != null) {
            try {
                process.getInputStream().close();
            } catch (Exception ignored) {
                log.debug("Error closing process input stream", ignored);
            }

            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {
                log.debug("Error closing process output stream", ignored);
            }

            try {
                process.getErrorStream().close();
            } catch (Exception ignored) {
                log.debug("Error closing process error stream", ignored);
            }
        }
    }

    private String readErrorStream(Process process) throws IOException {
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        } catch (IOException e) {
            log.warn("Failed to read error stream: {}", e.getMessage());
        }
        return errorOutput.toString();
    }
}