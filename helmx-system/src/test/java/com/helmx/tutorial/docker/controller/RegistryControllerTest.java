package com.helmx.tutorial.docker.controller;

import com.sun.net.httpserver.HttpServer;
import com.helmx.tutorial.docker.dto.RegistryCreateRequest;
import com.helmx.tutorial.docker.entity.Registry;
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

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void testConnectRegistry_authFailure_doesNotLeakCredentialsOrResponseDetails() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();

        try {
            RegistryConnectRequest request = new RegistryConnectRequest();
            request.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
            request.setUsername("admin");
            request.setPassword("secret@123");

            ResponseEntity<Result> response = registryController.TestConnectRegistry(request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Registry authentication failed", response.getBody().getMessage());
            assertFalse(response.getBody().toString().contains("secret@123"));
            assertFalse(response.getBody().toString().contains("admin"));
            assertFalse(response.getBody().toString().contains("401"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createRegistry_duplicateName_usesExistsCheckAndRejectsRequest() {
        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("DockerHub");
        request.setUrl("https://registry.example.com");

        when(registryMapper.exists(any())).thenReturn(true);

        ResponseEntity<Result> response = registryController.CreateRegistry(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registry name already exists", response.getBody().getMessage());
        verify(registryMapper).exists(any());
        verify(registryMapper, never()).insert(any(Registry.class));
    }

    @Test
    void createRegistry_success_persistsRegistryAfterExistsChecksPass() {
        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("DockerHub");
        request.setUrl("https://registry.example.com");
        request.setAuth(false);

        when(registryMapper.exists(any())).thenReturn(false);

        ResponseEntity<Result> response = registryController.CreateRegistry(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DockerHub", ((Registry) response.getBody().getData()).getName());
        verify(registryMapper).insert(any(Registry.class));
    }

    @Test
    void createRegistry_authEnabledWithoutPassword_rejectsRequest() {
        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("PrivateHub");
        request.setUrl("https://registry.example.com");
        request.setAuth(true);
        request.setUsername("admin");
        request.setPassword(null);

        when(registryMapper.exists(any())).thenReturn(false);

        ResponseEntity<Result> response = registryController.CreateRegistry(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username and password are required when authentication is enabled", response.getBody().getMessage());
        verify(registryMapper, never()).insert(any(Registry.class));
        verify(passwordUtil, never()).encrypt(any());
    }

    @Test
    void createRegistry_authEnabled_redactsPasswordInResponse() {
        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("PrivateHub");
        request.setUrl("https://registry.example.com");
        request.setAuth(true);
        request.setUsername("admin");
        request.setPassword("secret");

        when(registryMapper.exists(any())).thenReturn(false);
        when(passwordUtil.encrypt("secret")).thenReturn("encrypted-secret");

        ResponseEntity<Result> response = registryController.CreateRegistry(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Registry created = (Registry) response.getBody().getData();
        assertEquals("PrivateHub", created.getName());
        assertTrue(created.getAuth());
        assertEquals("admin", created.getUsername());
        assertNull(created.getPassword());
        verify(registryMapper).insert(any(Registry.class));
        verify(passwordUtil).encrypt("secret");
    }

    @Test
    void updateRegistry_authEnabledWithoutPassword_rejectsRequest() {
        Registry existing = new Registry();
        existing.setName("PrivateHub");
        existing.setUrl("https://registry.example.com");
        existing.setAuth(true);
        existing.setUsername("admin");
        existing.setPassword("encrypted");

        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("PrivateHub");
        request.setUrl("https://registry.example.com");
        request.setAuth(true);
        request.setUsername("admin");
        request.setPassword("");

        when(registryMapper.selectById(1L)).thenReturn(existing);

        ResponseEntity<Result> response = registryController.UpdateRegistryById(1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username and password are required when authentication is enabled", response.getBody().getMessage());
        verify(registryMapper, never()).updateById(any(Registry.class));
        verify(passwordUtil, never()).encrypt(any());
    }

    @Test
    void updateRegistry_missingAuth_preservesExistingCredentials() {
        Registry existing = new Registry();
        existing.setName("PrivateHub");
        existing.setUrl("https://registry.example.com");
        existing.setAuth(true);
        existing.setUsername("admin");
        existing.setPassword("encrypted");

        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("RenamedHub");
        request.setUrl("https://registry.example.com/v2");
        request.setAuth(null);

        when(registryMapper.selectById(2L)).thenReturn(existing);

        ResponseEntity<Result> response = registryController.UpdateRegistryById(2L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Registry updated = (Registry) response.getBody().getData();
        assertEquals("RenamedHub", updated.getName());
        assertEquals("https://registry.example.com/v2", updated.getUrl());
        assertTrue(updated.getAuth());
        assertEquals("admin", updated.getUsername());
        assertNull(updated.getPassword());
        verify(registryMapper).updateById(existing);
        assertTrue(existing.getAuth());
        assertEquals("admin", existing.getUsername());
        assertEquals("encrypted", existing.getPassword());
    }

    @Test
    void createRegistry_passwordRedactedInResponse() {
        // Bug fix: the create endpoint was returning the encrypted password in the response.
        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("PrivateReg");
        request.setUrl("https://private.example.com");
        request.setAuth(true);
        request.setUsername("admin");
        request.setPassword("secret123");

        when(registryMapper.exists(any())).thenReturn(false);
        when(passwordUtil.encrypt("secret123")).thenReturn("encrypted-secret");

        ResponseEntity<Result> response = registryController.CreateRegistry(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Registry result = (Registry) response.getBody().getData();
        assertNull(result.getPassword(), "Password must be redacted (null) in the response");
    }

    @Test
    void updateRegistry_nullAuthPreservesExistingCredentials() {
        // Bug fix: else branch was too broad – when auth is null the credentials were incorrectly wiped.
        Registry existing = new Registry();
        existing.setName("MyReg");
        existing.setUrl("https://registry.example.com");
        existing.setAuth(true);
        existing.setUsername("admin");
        existing.setPassword("encrypted-pw");

        when(registryMapper.selectById(1L)).thenReturn(existing);

        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("UpdatedName");
        // auth is intentionally left null – partial update, not touching credentials

        ResponseEntity<Result> response = registryController.UpdateRegistryById(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Registry result = (Registry) response.getBody().getData();
        assertEquals("UpdatedName", result.getName());
        // Credentials must be untouched because auth was null in the request
        assertTrue(result.getAuth(), "Auth flag must remain true");
        assertEquals("admin", result.getUsername(), "Username must not be cleared on partial update");
        // password is always redacted from the response
        assertNull(result.getPassword());
    }

    @Test
    void updateRegistry_explicitFalseAuthClearsCredentials() {
        // When auth is explicitly set to false, credentials should be cleared.
        Registry existing = new Registry();
        existing.setName("MyReg");
        existing.setUrl("https://registry.example.com");
        existing.setAuth(true);
        existing.setUsername("admin");
        existing.setPassword("encrypted-pw");

        when(registryMapper.selectById(2L)).thenReturn(existing);

        RegistryCreateRequest request = new RegistryCreateRequest();
        request.setName("MyReg");
        request.setAuth(false);

        ResponseEntity<Result> response = registryController.UpdateRegistryById(2L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Registry result = (Registry) response.getBody().getData();
        assertFalse(result.getAuth(), "Auth flag must be set to false");
        assertNull(result.getUsername(), "Username must be cleared when auth is explicitly false");
        assertNull(result.getPassword(), "Password must be cleared when auth is explicitly false");
    }
}