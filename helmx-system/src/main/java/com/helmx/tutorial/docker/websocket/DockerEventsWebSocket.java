package com.helmx.tutorial.docker.websocket;

import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.EventsCmd;
import com.github.dockerjava.api.model.Event;
import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.docker.utils.DockerHostValidator;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;
import com.helmx.tutorial.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker Events WebSocket Handler
 * Streams real-time Docker daemon events to connected clients.
 * Clients can filter events by type (container, image, network, volume, daemon)
 * and by action (start, stop, die, create, destroy, etc.).
 *
 * Connect via: /api/v1/ops/events?token=<jwt>&host=<dockerHost>[&type=container][&action=start,stop]
 */
@Slf4j
@Component
public class DockerEventsWebSocket extends TextWebSocketHandler {

    @Autowired
    private DockerClientUtil dockerClientUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DockerHostValidator dockerHostValidator;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final Map<String, EventsCmd> activeEventsCmds = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("Docker Events WebSocket connection established: {}", session.getId());

        String token = extractParam(session, "token", null);
        if (token == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing token"));
            return;
        }

        if (!jwtTokenUtil.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
            return;
        }

        Long userId = getUserIdFromToken(token);
        if (!checkPermission(userId)) {
            log.warn("User {} does not have permission to stream Docker events", userId);
            session.close(CloseStatus.BAD_DATA.withReason("Forbidden"));
            return;
        }

        String host = extractParam(session, "host", null);
        if (host == null || host.isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing host parameter"));
            return;
        }

        try {
            dockerHostValidator.validateHostAllowlist(host);
        } catch (IllegalArgumentException ex) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized host"));
            return;
        }

        // Optional filters
        String typeFilter = extractParam(session, "type", null);
        String actionFilter = extractParam(session, "action", null);

        startEventStreaming(session, host, typeFilter, actionFilter);
    }

    private void startEventStreaming(WebSocketSession session, String host, String typeFilter, String actionFilter) {
        try {
            dockerClientUtil.setCurrentHost(host);
            DockerClient dockerClient = dockerClientUtil.getCurrentDockerClient();

            EventsCmd eventsCmd = dockerClient.eventsCmd();

            // Apply optional filters
            if (typeFilter != null && !typeFilter.isEmpty()) {
                eventsCmd.withEventTypeFilter(typeFilter);
            }
            if (actionFilter != null && !actionFilter.isEmpty()) {
                String[] actions = actionFilter.split(",");
                eventsCmd.withEventFilter(actions);
            }

            activeEventsCmds.put(session.getId(), eventsCmd);

            eventsCmd.exec(new ResultCallback.Adapter<Event>() {
                @Override
                public void onNext(Event event) {
                    if (!session.isOpen()) {
                        return;
                    }
                    try {
                        JSONObject eventJson = new JSONObject();
                        eventJson.put("type", event.getType() != null ? event.getType().getValue() : null);
                        eventJson.put("action", event.getAction());
                        eventJson.put("actor", event.getActor() != null
                                ? Map.of("id", Objects.toString(event.getActor().getId(), ""),
                                         "attributes", event.getActor().getAttributes() != null
                                                 ? event.getActor().getAttributes() : Map.of())
                                : null);
                        eventJson.put("time", event.getTime());
                        eventJson.put("timeNano", event.getTimeNano());
                        synchronized (session) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(eventJson.toJSONString()));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error sending Docker event via WebSocket: {}", session.getId(), e);
                    }
                }

                @Override
                public void onComplete() {
                    log.info("Docker events stream completed for session: {}", session.getId());
                    cleanupSession(session.getId());
                    closeSession(session);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Docker events stream error for session: {}", session.getId(), throwable);
                    cleanupSession(session.getId());
                    closeSessionWithError(session);
                }
            });
        } catch (Exception e) {
            log.error("Failed to start Docker events streaming for session: {}", session.getId(), e);
            cleanupSession(session.getId());
            closeSessionWithError(session);
        } finally {
            // 清除ThreadLocal，避免线程池复用时的host泄漏
            dockerClientUtil.clearCurrentHost();
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        // No-op: clients only listen for events, no input handling needed
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        log.info("Docker Events WebSocket connection closed: {}, status: {}", session.getId(), status);
        cleanupSession(session.getId());
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        log.error("Docker Events WebSocket transport error: {}", session.getId(), exception);
        cleanupSession(session.getId());
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR.withReason("Transport error"));
            }
        } catch (Exception e) {
            log.error("Error closing session on transport error", e);
        }
    }

    private void cleanupSession(String sessionId) {
        EventsCmd cmd = activeEventsCmds.remove(sessionId);
        if (cmd != null) {
            try {
                cmd.close();
            } catch (Exception e) {
                log.debug("Error closing EventsCmd for session: {}", sessionId, e);
            }
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.debug("Error closing WebSocket session: {}", session.getId(), e);
        }
    }

    private void closeSessionWithError(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.debug("Error closing WebSocket session with error: {}", session.getId(), e);
        }
    }

    private String extractParam(WebSocketSession session, String name, String defaultValue) {
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith(name + "=")) {
                    try {
                        return java.net.URLDecoder.decode(param.substring(name.length() + 1), java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.error("Failed to decode parameter: {}", name, e);
                    }
                }
            }
        }
        return defaultValue;
    }

    private boolean checkPermission(Long userId) {
        if (userId != null) {
            if (userService.isSuperAdmin(userId)) {
                return true;
            }
            Set<String> userPermissions = userMapper.selectUserPermissions(userId);
            return userPermissions.contains("Ops:Container:List") || userPermissions.contains("Ops:Events:List");
        }
        return false;
    }

    private Long getUserIdFromToken(String token) {
        Object userIdClaim = jwtTokenUtil.getClaimFromToken(token, "userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim != null) {
            try {
                return Long.valueOf(userIdClaim.toString());
            } catch (NumberFormatException ex) {
                log.warn("Invalid userId claim in websocket token: {}", userIdClaim);
            }
        }
        return null;
    }
}
