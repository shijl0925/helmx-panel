package com.helmx.tutorial.security.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${app.jwt.public-key-path}")
    private String publicKeyPath;

    private final ResourceLoader resourceLoader;

    public JwtConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public JwtEncoder jwtEncoder() throws Exception {
        // 加载私钥
        Resource privateKeyResource = resourceLoader.getResource(privateKeyPath);
        String privateKeyStr = StreamUtils.copyToString(privateKeyResource.getInputStream(), StandardCharsets.UTF_8);
        byte[] privateKeyBytes = Base64.getDecoder().decode(extractKeyContent(privateKeyStr));
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        // 加载公钥
        Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
        String publicKeyStr = StreamUtils.copyToString(publicKeyResource.getInputStream(), StandardCharsets.UTF_8);
        byte[] publicKeyBytes = Base64.getDecoder().decode(extractKeyContent(publicKeyStr));
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        // 创建 RSA 密钥对，需要同时包含公钥和私钥
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        // 创建 JWKSource
        JWKSource<SecurityContext> jwkSource = (jwkSelector, context) ->
                jwkSelector.select(new JWKSet(rsaKey));

        // 返回 NimbusJwtEncoder 实例
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() throws Exception {
        // 加载公钥
        Resource publicKeyResource = resourceLoader.getResource(publicKeyPath);
        String publicKeyStr = StreamUtils.copyToString(publicKeyResource.getInputStream(), StandardCharsets.UTF_8);
        byte[] publicKeyBytes = Base64.getDecoder().decode(extractKeyContent(publicKeyStr));
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        // 创建 RSA 密钥对
        RSAKey rsaKey = new RSAKey.Builder(publicKey).build();

        // 返回 NimbusJwtDecoder 实例
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    /**
     * 从 PEM 格式的密钥中提取 Base64 编码的内容，去除标头和标尾
     * @param pemContent PEM 格式的密钥内容
     * @return 纯 Base64 编码的密钥内容
     */
    private String extractKeyContent(String pemContent) {
        String[] lines = pemContent.split("\n");
        StringBuilder content = new StringBuilder();
        boolean inKey = false;

        for (String line : lines) {
            if (line.startsWith("-----BEGIN")) {
                inKey = true;
                continue;
            }
            if (line.startsWith("-----END")) {
                inKey = false;
                break;
            }
            if (inKey) {
                content.append(line.trim());
            }
        }

        return content.toString();
    }
}