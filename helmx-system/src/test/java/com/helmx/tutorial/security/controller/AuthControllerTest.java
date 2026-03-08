package com.helmx.tutorial.security.controller;

import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.security.security.service.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JWTService jwtService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController();
        ReflectionTestUtils.setField(authController, "jwtService", jwtService);
    }

    @Test
    void refreshToken_missingAuthorizationHeader_returnsBadRequest() {
        ResponseEntity<Result> response = authController.refreshToken(new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Missing token", response.getBody().getMessage());
    }

    @Test
    void refreshToken_deniedByService_returnsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        when(jwtService.refreshToken("expired-token")).thenThrow(new IllegalArgumentException("denied"));

        ResponseEntity<Result> response = authController.refreshToken(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Token refresh denied. Please login again.", response.getBody().getMessage());
    }
}
