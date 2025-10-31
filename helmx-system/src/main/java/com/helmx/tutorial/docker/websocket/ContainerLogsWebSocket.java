package com.helmx.tutorial.docker.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import com.helmx.tutorial.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContainerLogsWebSocket extends TextWebSocketHandler {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    private final Map<String, LogContainerCmd> activeCommands = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // 连接建立后的处理
        log.info("WebSocket connection established for session: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // 处理客户端发送的消息（包含容器日志请求参数）
        String payload = message.getPayload();
        // 解析payload获取containerId, host, tail等参数
        JSONObject request = JSON.parseObject(payload);

        String host = request.getString("host");
        String containerId = request.getString("containerId");
        int tail = request.getIntValue("tail", 0);

        String token = request.getString("token");

        // 获取当前用户名, 检查权限
        Long userId = SecurityUtils.getCurrentUserId(token);
        if (!checkPermission(userId)) {
            log.warn("User {} does not have permission to access terminal", userId);
            session.close(CloseStatus.BAD_DATA.withReason("Forbidden"));
            return;
        }

        // 设置Docker主机
        dockerClientUtil.setCurrentHost(host);

        // 开始流式传输日志
        streamContainerLogs(session, containerId, tail);
    }

    private void streamContainerLogs(WebSocketSession session, String containerId, int tail) {
        LogContainerCmd cmd = null;
        try {
            cmd = dockerClientUtil.getCurrentDockerClient()
                    .logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true);

            if (tail != 0) {
                cmd.withTail(tail);
            }

            // 存储活动命令
            activeCommands.put(session.getId(), cmd);

            final LogContainerCmd finalCmd = cmd;
            cmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(new String(item.getPayload())));
                        }
                    } catch (Exception e) {
                        log.error("Error sending log message via WebSocket", e);
                        // 出错时尝试关闭命令
                        try {
                            finalCmd.close();
                        } catch (Exception closeException) {
                            log.error("Error closing command", closeException);
                        }
                    }
                }

                @Override
                public void onComplete() {
                    activeCommands.remove(session.getId());

                    try {
                        if (session.isOpen()) {
                            session.close();
                        }
                    } catch (Exception e) {
                        log.error("Error closing WebSocket session", e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    activeCommands.remove(session.getId());

                    log.error("Error streaming container logs", throwable);
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage("Error: " + throwable.getMessage()));
                            session.close(CloseStatus.SERVER_ERROR);
                        }
                    } catch (Exception e) {
                        log.error("Error closing WebSocket session on error", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to start log streaming for container: {}", containerId, e);
            try {
                if (cmd != null) {
                    cmd.close();
                }
                if (session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR);
                }
            } catch (Exception closeException) {
                log.error("Error closing resources", closeException);
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("WebSocket connection closed for session: {}, status: {}", session.getId(), status);

        // 清理活动的命令
        LogContainerCmd cmd = activeCommands.remove(session.getId());
        if (cmd != null) {
            try {
                cmd.close();
            } catch (Exception e) {
                log.error("Error closing LogContainerCmd for session: {}", session.getId(), e);
            }
        }
    }

    private boolean checkPermission(Long userId) {
        if (userId != null) {
            if (userService.isSuperAdmin(userId)) {
                return true;
            }

            Set<String> userPermissions = userMapper.selectUserPermissions(userId);
            return userPermissions.contains("Ops:Container:Logs");
        }
        return false;
    }
}
