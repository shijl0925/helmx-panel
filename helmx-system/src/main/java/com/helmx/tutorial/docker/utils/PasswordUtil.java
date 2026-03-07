package com.helmx.tutorial.docker.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordUtil {
    private static final String ALGORITHM = "AES";
    // GCM模式：提供认证加密，防止填充预言攻击
    private static final String TRANSFORMATION_GCM = "AES/GCM/NoPadding";
    // CBC模式保留，仅用于解密历史数据（向后兼容）
    private static final String TRANSFORMATION_CBC = "AES/CBC/PKCS5Padding";
    // GCM模式的版本前缀，用于区分新旧加密格式
    private static final String GCM_PREFIX = "v2:";
    private static final int GCM_IV_LENGTH = 12; // GCM推荐96位IV
    private static final int GCM_TAG_LENGTH = 128; // GCM认证标签长度（位）

    @Value("${docker.password.secret-key:bDAORZ9t7/+1RpyV5JIXJg==}")
    private String secretKeyValue;

    private byte[] keyBytes;

    @PostConstruct
    private void init() {
        keyBytes = Base64.getDecoder().decode(secretKeyValue);
        int keyLen = keyBytes.length;
        if (keyLen != 16 && keyLen != 24 && keyLen != 32) {
            throw new IllegalArgumentException(
                "Invalid AES key length: " + keyLen + " bytes. Must be 16, 24, or 32 bytes.");
        }
    }

    /**
     * 加密密码（使用AES/GCM认证加密）
     * @param plainText 明文密码
     * @return 加密后的密码（格式：v2:Base64(IV+密文+认证标签)）
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);

            // 生成随机IV（GCM推荐96位/12字节）
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            // GCM模式下，doFinal输出为密文+认证标签
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文（含认证标签）合并存储
            byte[] combined = new byte[GCM_IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, combined, GCM_IV_LENGTH, encryptedBytes.length);

            return GCM_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("encrypt failed", e);
        }
    }

    /**
     * 解密密码
     * 支持新格式（v2:，AES/GCM）和旧格式（AES/CBC，向后兼容）
     * @param cipherText 加密后的密码
     * @return 明文密码
     */
    public String decrypt(String cipherText) {
        if (cipherText != null && cipherText.startsWith(GCM_PREFIX)) {
            return decryptGcm(cipherText.substring(GCM_PREFIX.length()));
        }
        return decryptCbc(cipherText);
    }

    /**
     * 使用AES/GCM解密（新格式）
     */
    private String decryptGcm(String base64CipherText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);

            byte[] combined = Base64.getDecoder().decode(base64CipherText);

            // 提取IV和密文（含GCM认证标签）
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt failed", e);
        }
    }

    /**
     * 使用AES/CBC解密（旧格式，向后兼容）
     * @deprecated 仅用于解密历史数据，新数据请使用encrypt()加密
     */
    @Deprecated
    private String decryptCbc(String cipherText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_CBC);

            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 提取IV和密文（CBC模式IV为16字节）
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt failed", e);
        }
    }

    /**
     * 生成新的AES密钥（可用于替换默认密钥）
     * @return Base64编码的密钥字符串
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(128);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("generate key failed", e);
        }
    }
}
