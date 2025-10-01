package com.helmx.tutorial.docker.websocket;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class TerminalSession {

    private final String host;
    private final String cmd;
    private final String user;
    private final String containerId;
    private final DockerClientUtil dockerClientUtil;

    @Getter
    private String execId;

    private ExecStartCmd execStartCmd;
    private PipedOutputStream stdin;
    private PipedInputStream stdinPipe;

    @Getter
    private volatile boolean running = false;

    private WebSocketSession webSocketSession;

    public TerminalSession(String host, String containerId, DockerClientUtil dockerClientUtil) {
        this(host, containerId, dockerClientUtil, "/bin/bash", "root");
    }

    public TerminalSession(String host, String containerId, DockerClientUtil dockerClientUtil, String cmd, String user) {
        this.host = host;
        this.cmd = cmd != null ? cmd : "/bin/bash";  // 默认值
        this.user = user != null ? user : "root";    // 默认值
        this.containerId = containerId;
        this.dockerClientUtil = dockerClientUtil;
    }

    public void start(WebSocketSession webSocketSession) throws Exception {
        this.webSocketSession = webSocketSession;
        this.running = true;

        // 设置Docker客户端
        dockerClientUtil.setCurrentHost(host);
        DockerClient dockerClient = dockerClientUtil.getCurrentDockerClient();

        // 检查容器是否正在运行
        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            if (!Boolean.TRUE.equals(containerInfo.getState().getRunning())) {
                log.error("Container {} was not running", containerId);
                webSocketSession.sendMessage(new TextMessage("Error: Container is not running\r\n"));
                webSocketSession.close(CloseStatus.SERVICE_RESTARTED);
                return;
            }
        } catch (Exception e) {
            log.error("Failed to check or start container {}: {}", containerId, e.getMessage());
            webSocketSession.sendMessage(new TextMessage("Error: Failed to check or start container\r\n"));
            webSocketSession.close(CloseStatus.SERVICE_RESTARTED);
            return;
        }

        // 创建exec命令
        try {
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmd)
                    .withUser(user)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .exec();

            this.execId = execCreateCmdResponse.getId();
            log.debug("Created exec command with ID: {} for container: {}", execId, containerId);
        } catch (Exception e) {
            log.error("Failed to create exec command for container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to initialize terminal session", e);
        }

        // 创建管道用于输入
        this.stdinPipe = new PipedInputStream();
        this.stdin = new PipedOutputStream(stdinPipe);

        // 启动exec命令
        this.execStartCmd = dockerClient.execStartCmd(execId)
                .withDetach(false)
                .withTty(true)
                .withStdIn(stdinPipe);

        // 启动异步处理输出
        ExecStartResultCallback callback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                if (frame != null && frame.getPayload() != null && running) {
                    try {
                        // 将容器输出发送到前端
                        String output = new String(frame.getPayload(), StandardCharsets.UTF_8);
                        webSocketSession.sendMessage(new TextMessage(output));
                    } catch (Exception e) {
                        log.error("Error sending output to WebSocket", e);
                    }
                }
            }

            @Override
            public void onComplete() {
                log.debug("Terminal session completed for exec ID: {}", execId);
                cleanup();
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Terminal session error for exec ID: {}", execId, throwable);
                cleanup();
            }

            private void cleanup() {
                try {
                    close();
                } catch (Exception e) {
                    log.error("Error during terminal session cleanup", e);
                }

                try {
                    if (webSocketSession != null && webSocketSession.isOpen()) {
                        webSocketSession.close();
                    }
                } catch (Exception e) {
                    log.error("Error closing WebSocket session", e);
                }
            }
        };

        execStartCmd.exec(callback);
    }

    public void sendInput(String input) throws Exception {
        if (running && stdin != null) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }
    }

    public void close() {
        running = false;
        closeResource(stdin);
        closeResource(stdinPipe);
    }

    private void closeResource(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                log.debug("Error closing resource", e);
            }
        }
    }
}
