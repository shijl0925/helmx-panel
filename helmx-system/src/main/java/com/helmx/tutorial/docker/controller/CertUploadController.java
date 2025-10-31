package com.helmx.tutorial.docker.controller;

import com.helmx.tutorial.docker.utils.PathUtil;
import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ops/certs")
public class CertUploadController {

    @Value("${docker.cert.upload.path:docker-certs/}")
    private String certUploadPath;

    @Operation(summary = "Upload Docker TLS certificates")
    @PostMapping("/upload")
    public ResponseEntity<Result> uploadCertFiles(
            @RequestParam("caCert") MultipartFile caCert,
            @RequestParam("clientCert") MultipartFile clientCert,
            @RequestParam("clientKey") MultipartFile clientKey,
            @RequestParam() String name) {

        try {
            // 加强输入验证和清理
            String certDirName = PathUtil.sanitizeDirectoryName(name);
            if (certDirName == null || certDirName.isEmpty()) {
                return ResponseUtil.failed(400, null, "Invalid certificate directory name");
            }
            log.info("Uploading certificates for environment: {}", certDirName);

            // 构建路径并验证是否在允许范围内
            Path basePath = Paths.get(certUploadPath).toAbsolutePath().normalize();
            log.info("Base directory path: {}", basePath);
            Path certDirPath = basePath.resolve(certDirName).resolve("certs").normalize();

            // 关键安全检查：确保路径在基础目录内
            if (!certDirPath.startsWith(basePath)) {
                return ResponseUtil.failed(400, null, "Invalid certificate directory name");
            }
            log.info("Certificate directory path: {}", certDirPath);

            Files.createDirectories(certDirPath);

            // 保存证书文件
            saveCertFile(caCert, certDirPath, "ca.pem");
            saveCertFile(clientCert, certDirPath, "cert.pem");
            saveCertFile(clientKey, certDirPath, "key.pem");

            String certPath = certDirPath.toString();

            // 返回证书路径
            return ResponseUtil.success("Certificates uploaded successfully",
                    Map.of("certPath", certPath, "envName", certDirName));

        } catch (Exception e) {
            log.error("Failed to upload certificates", e);
            return ResponseUtil.failed(500, null, "Failed to upload certificates: " + e.getMessage());
        }
    }

    private void saveCertFile(MultipartFile file, Path dirPath, String fileName) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            // 设置适当的文件权限
            filePath.toFile().setReadable(true, true);
            if (fileName.equals("key.pem")) {
                filePath.toFile().setWritable(false, false);
                filePath.toFile().setReadable(true, true);
            }
        }
    }
}
