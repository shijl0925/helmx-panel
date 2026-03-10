package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.docker.utils.PasswordUtil;
import com.helmx.tutorial.dto.Result;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistryControllerCatalogTest {

    @Mock
    private RegistryMapper registryMapper;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private RegistryController registryController;

    private HttpServer server;
    private int serverPort;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = server.getAddress().getPort();

        server.createContext("/v2/_catalog", exchange -> {
            byte[] body = "{\"repositories\":[\"myapp\",\"mydb\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/v2/myapp/tags/list", exchange -> {
            byte[] body = "{\"name\":\"myapp\",\"tags\":[\"latest\",\"v1.0\",\"v2.0\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/v2/mydb/tags/list", exchange -> {
            byte[] body = "{\"name\":\"mydb\",\"tags\":[\"15\",\"15.2\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getRegistryCatalog_returnsRepositoriesWithTags() {
        Registry registry = new Registry();
        ReflectionTestUtils.setField(registry, "id", 1L);
        registry.setName("local");
        registry.setUrl("http://127.0.0.1:" + serverPort);
        registry.setAuth(false);

        when(registryMapper.selectById(1L)).thenReturn(registry);

        ResponseEntity<Result> response = registryController.GetRegistryCatalog(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());

        Object data = response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.toString().contains("myapp"));
        assertTrue(data.toString().contains("mydb"));
    }

    @Test
    void getRegistryCatalog_returnsNotFoundWhenRegistryMissing() {
        when(registryMapper.selectById(99L)).thenReturn(null);

        ResponseEntity<Result> response = registryController.GetRegistryCatalog(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Registry not found", response.getBody().getMessage());
    }

    @Test
    void getRegistryCatalog_returnsErrorForInvalidUrl() {
        Registry registry = new Registry();
        ReflectionTestUtils.setField(registry, "id", 2L);
        registry.setName("bad");
        registry.setUrl("://bad-url");
        registry.setAuth(false);

        when(registryMapper.selectById(2L)).thenReturn(registry);

        ResponseEntity<Result> response = registryController.GetRegistryCatalog(2L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid registry URL format", response.getBody().getMessage());
    }

    @Test
    void getRegistryCatalog_usesDecryptedCredentialsForAuthRegistry() {
        Registry registry = new Registry();
        ReflectionTestUtils.setField(registry, "id", 3L);
        registry.setName("private");
        registry.setUrl("http://127.0.0.1:" + serverPort);
        registry.setAuth(true);
        registry.setUsername("admin");
        registry.setPassword("encrypted-password");

        when(registryMapper.selectById(3L)).thenReturn(registry);
        when(passwordUtil.decrypt("encrypted-password")).thenReturn("secret");

        ResponseEntity<Result> response = registryController.GetRegistryCatalog(3L);

        // The server doesn't validate auth, but the call should succeed
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCode());
    }
}
