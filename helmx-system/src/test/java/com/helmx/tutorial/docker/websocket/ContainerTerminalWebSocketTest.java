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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerTerminalWebSocketTest {

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

    private ContainerTerminalWebSocket handler;

    @BeforeEach
    void setUp() {
        handler = new ContainerTerminalWebSocket();
        ReflectionTestUtils.setField(handler, "dockerClientUtil", dockerClientUtil);
        ReflectionTestUtils.setField(handler, "userPermissionService", userPermissionService);
        ReflectionTestUtils.setField(handler, "dockerHostValidator", dockerHostValidator);
        ReflectionTestUtils.setField(handler, "jwtTokenUtil", jwtTokenUtil);
    }

    @Test
    void afterConnectionEstablished_invalidToken_closesSession() throws Exception {
        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/v1/ops/containers/terminal/container-1?token=expired&host=tcp://docker:2375"));
        when(jwtTokenUtil.getValidJwt("expired")).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status ->
                status.getCode() == CloseStatus.POLICY_VIOLATION.getCode()
                        && "Invalid or expired token".equals(status.getReason())));
        verifyNoInteractions(dockerHostValidator, userPermissionService);
    }

    @Test
    void afterConnectionEstablished_unauthorizedHost_closesSession() throws Exception {
        when(session.getId()).thenReturn("session-2");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/v1/ops/containers/terminal/container-1?token=valid&host=tcp://blocked:2375"));
        Jwt jwt = Jwt.withTokenValue("valid")
                .header("alg", "RS256")
                .subject("alice")
                .claim("userId", 7L)
                .build();
        when(jwtTokenUtil.getValidJwt("valid")).thenReturn(jwt);
        when(jwtTokenUtil.getUserIdFromJwt(jwt)).thenReturn(7L);
        when(userPermissionService.hasPermission(7L, "Ops:Container:Exec")).thenReturn(true);
        doThrow(new IllegalArgumentException("blocked"))
                .when(dockerHostValidator).validateHostAllowlist("tcp://blocked:2375");

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status ->
                status.getCode() == CloseStatus.POLICY_VIOLATION.getCode()
                        && "Unauthorized host".equals(status.getReason())));
        verify(dockerHostValidator).validateHostAllowlist("tcp://blocked:2375");
        verify(dockerClientUtil, never()).clearCurrentHost();
    }
}
