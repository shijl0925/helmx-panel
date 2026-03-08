package com.helmx.tutorial.docker.websocket;

import com.helmx.tutorial.docker.utils.DockerClientUtil;
import com.helmx.tutorial.docker.utils.DockerHostValidator;
import com.helmx.tutorial.security.security.service.UserPermissionService;
import com.helmx.tutorial.utils.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerEventsWebSocketTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    @Mock
    private UserPermissionService userPermissionService;

    @Mock
    private DockerHostValidator dockerHostValidator;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private WebSocketSession session;

    private DockerEventsWebSocket handler;

    @BeforeEach
    void setUp() {
        handler = new DockerEventsWebSocket();
        ReflectionTestUtils.setField(handler, "dockerClientUtil", dockerClientUtil);
        ReflectionTestUtils.setField(handler, "userPermissionService", userPermissionService);
        ReflectionTestUtils.setField(handler, "dockerHostValidator", dockerHostValidator);
        ReflectionTestUtils.setField(handler, "jwtTokenUtil", jwtTokenUtil);
    }

    @Test
    void afterConnectionEstablished_nullUri_closesSessionAsMissingToken() throws Exception {
        when(session.getId()).thenReturn("events-1");
        when(session.getUri()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status ->
                status.getCode() == CloseStatus.BAD_DATA.getCode()
                        && "Missing token".equals(status.getReason())));
        verifyNoInteractions(jwtTokenUtil, dockerHostValidator, userPermissionService, dockerClientUtil);
    }
}
