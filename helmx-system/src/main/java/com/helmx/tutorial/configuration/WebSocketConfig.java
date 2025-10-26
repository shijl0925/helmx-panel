package com.helmx.tutorial.configuration;

import com.helmx.tutorial.docker.websocket.ContainerLogsWebSocket;
import com.helmx.tutorial.docker.websocket.ContainerTerminalWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
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

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(containerTerminalWebSocket, "/api/v1/ops/containers/terminal/{containerId}")
                .addHandler(containerLogsWebSocket, "/api/v1/ops/containers/logs/stream")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
