package com.helmx.tutorial.configuration;

import com.helmx.tutorial.docker.websocket.ContainerLogsWebSocket;
import com.helmx.tutorial.docker.websocket.ContainerTerminalWebSocket;
import com.helmx.tutorial.docker.websocket.DockerEventsWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    private final ContainerTerminalWebSocket containerTerminalWebSocket = mock(ContainerTerminalWebSocket.class);
    private final ContainerLogsWebSocket containerLogsWebSocket = mock(ContainerLogsWebSocket.class);
    private final DockerEventsWebSocket dockerEventsWebSocket = mock(DockerEventsWebSocket.class);
    private final WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
    private final WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig();
        ReflectionTestUtils.setField(webSocketConfig, "containerTerminalWebSocket", containerTerminalWebSocket);
        ReflectionTestUtils.setField(webSocketConfig, "containerLogsWebSocket", containerLogsWebSocket);
        ReflectionTestUtils.setField(webSocketConfig, "dockerEventsWebSocket", dockerEventsWebSocket);
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigin", "https://ui.example.com,https://admin.example.com");
        when(registry.addHandler(containerTerminalWebSocket, "/api/v1/ops/containers/terminal/{containerId}"))
                .thenReturn(registration);
        when(registration.addHandler(containerLogsWebSocket, "/api/v1/ops/containers/logs/stream"))
                .thenReturn(registration);
        when(registration.addHandler(dockerEventsWebSocket, "/api/v1/ops/events"))
                .thenReturn(registration);
        when(registration.setAllowedOrigins("https://ui.example.com", "https://admin.example.com"))
                .thenReturn(registration);
    }

    @Test
    void registerWebSocketHandlers_usesConfiguredOrigins() {
        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registration).setAllowedOrigins("https://ui.example.com", "https://admin.example.com");
    }

    @Test
    void registerWebSocketHandlers_missingConfiguredOrigins_throwsIllegalStateException() {
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigin", "");

        assertThrows(IllegalStateException.class, () -> webSocketConfig.registerWebSocketHandlers(registry));
    }
}
