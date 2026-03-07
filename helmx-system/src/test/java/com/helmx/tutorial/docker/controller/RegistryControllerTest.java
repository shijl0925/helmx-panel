package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.dto.RegistryConnectRequest;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.docker.utils.PasswordUtil;
import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class RegistryControllerTest {

    @Mock
    private RegistryMapper registryMapper;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private RegistryController registryController;

    @Test
    void testConnectRegistry_invalidUrl_doesNotLeakCredentialsOrExceptionDetails() throws Exception {
        RegistryConnectRequest request = new RegistryConnectRequest();
        request.setUrl("://bad url");
        request.setUsername("admin");
        request.setPassword("secret@123");

        ResponseEntity<Result> response = registryController.TestConnectRegistry(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid registry URL format", response.getBody().getMessage());
        assertFalse(response.getBody().toString().contains("secret@123"));
        assertFalse(response.getBody().toString().contains("admin"));
    }
}
