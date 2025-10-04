package com.helmx.tutorial.docker.websocket;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContainerTerminalWebSocket extends TextWebSocketHandler {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    // 维护会话映射
    private final ConcurrentHashMap<String, TerminalSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Terminal WebSocket connection established: {}", session.getId());

        // 提取 token（从查询参数或头信息）
        String token = extractParameterFromQuery(session, "token", null);
        if (token == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing token"));
            return;
        }

        // 从URL路径中提取containerId
        String containerId = extractContainerIdFromPath(session);
        if (containerId == null || containerId.isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid container ID"));
            return;
        }

        // 从URL查询参数中提取host
        String host = extractParameterFromQuery(session, "host", null);
        if (host == null || host.isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid host parameter"));
            return;
        }

        String cmd = extractParameterFromQuery(session, "cmd", "/bin/bash");
        String user = extractParameterFromQuery(session, "user", "root");

        // 创建终端会话
        TerminalSession terminalSession = new TerminalSession(host, containerId, dockerClientUtil, cmd, user);
        sessions.put(session.getId(), terminalSession);

        // 启动终端会话
        terminalSession.start(session);
    }

    private String extractParameterFromQuery(WebSocketSession session, String paramName, String defaultValue) {
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith(paramName + "=")) {
                    try {
                        return java.net.URLDecoder.decode(param.substring(paramName.length() + 1), "UTF-8");
                    } catch (Exception e) {
                        log.error("Failed to decode {} parameter", paramName, e);
                    }
                }
            }
        }
        return defaultValue;
    }

    private String extractContainerIdFromPath(WebSocketSession session) {
        String path = session.getUri().getPath();
        // 提取路径中的containerId，例如：/api/v1/ops/containers/terminal/{containerId}
        String[] pathParts = path.split("/");
        if (pathParts.length > 0) {
            return pathParts[pathParts.length - 1];
        }
        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TerminalSession terminalSession = sessions.get(session.getId());
        if (terminalSession != null) {
            // 将前端输入发送到容器
            terminalSession.sendInput(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Terminal WebSocket connection closed: {}", session.getId());
        TerminalSession terminalSession = sessions.remove(session.getId());
        if (terminalSession != null) {
            terminalSession.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Terminal WebSocket transport error: {}", session.getId(), exception);
        try {
            TerminalSession terminalSession = sessions.remove(session.getId());
            if (terminalSession != null) {
                terminalSession.close();
            }
        } finally {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR.withReason("Transport error occurred"));
            }
        }
    }
}
