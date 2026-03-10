package com.helmx.tutorial.docker.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CertUploadControllerIntegrationTest {

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CertUploadController controller = new CertUploadController();
        ReflectionTestUtils.setField(controller, "certUploadPath", tempDir.toString());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadCertFiles_sanitizesDirectoryNameAndStoresExpectedFiles() throws Exception {
        MockMultipartFile caCert = new MockMultipartFile("caCert", "ca.pem", "application/x-pem-file", "ca-content".getBytes());
        MockMultipartFile clientCert = new MockMultipartFile("clientCert", "cert.pem", "application/x-pem-file", "cert-content".getBytes());
        MockMultipartFile clientKey = new MockMultipartFile("clientKey", "key.pem", "application/x-pem-file", "key-content".getBytes());

        mockMvc.perform(multipart("/api/v1/ops/certs/upload")
                        .file(caCert)
                        .file(clientCert)
                        .file(clientKey)
                        .param("name", "../prod?*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Certificates uploaded successfully"))
                .andExpect(jsonPath("$.data.envName").value("prod"));

        Path certDir = tempDir.resolve("prod").resolve("certs");
        assertTrue(Files.exists(certDir.resolve("ca.pem")));
        assertTrue(Files.exists(certDir.resolve("cert.pem")));
        assertTrue(Files.exists(certDir.resolve("key.pem")));
    }

    @Test
    void uploadCertFiles_rejectsInvalidDirectoryName() throws Exception {
        MockMultipartFile emptyCa = new MockMultipartFile("caCert", "ca.pem", "application/x-pem-file", "ca".getBytes());
        MockMultipartFile emptyClientCert = new MockMultipartFile("clientCert", "cert.pem", "application/x-pem-file", "cert".getBytes());
        MockMultipartFile emptyClientKey = new MockMultipartFile("clientKey", "key.pem", "application/x-pem-file", "key".getBytes());

        mockMvc.perform(multipart("/api/v1/ops/certs/upload")
                        .file(emptyCa)
                        .file(emptyClientCert)
                        .file(emptyClientKey)
                        .param("name", "../.."))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid certificate directory name"));
    }
}
