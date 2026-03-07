package com.helmx.tutorial.configuration;

import com.helmx.tutorial.docker.websocket.ContainerLogsWebSocket;
import com.helmx.tutorial.docker.websocket.ContainerTerminalWebSocket;
import com.helmx.tutorial.docker.websocket.DockerEventsWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ContainerTerminalWebSocket containerTerminalWebSocket;

    @Autowired
    private ContainerLogsWebSocket containerLogsWebSocket;

    @Autowired
    private DockerEventsWebSocket dockerEventsWebSocket;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        if (!StringUtils.hasText(allowedOrigin)) {
            throw new IllegalStateException("app.cors.allowed-origin must be configured for WebSocket origins");
        }
        String[] allowedOrigins = StringUtils.commaDelimitedListToStringArray(allowedOrigin);
        registry.addHandler(containerTerminalWebSocket, "/api/v1/ops/containers/terminal/{containerId}")
                .addHandler(containerLogsWebSocket, "/api/v1/ops/containers/logs/stream")
                .addHandler(dockerEventsWebSocket, "/api/v1/ops/events")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
