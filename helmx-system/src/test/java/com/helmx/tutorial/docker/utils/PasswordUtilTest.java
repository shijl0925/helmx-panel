package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() throws Exception {
        passwordUtil = new PasswordUtil();
        // Inject a test-only AES key via reflection (base64 of "TestPasswordSalt")
        Field field = PasswordUtil.class.getDeclaredField("secretKeyValue");
        field.setAccessible(true);
        field.set(passwordUtil, "VGVzdFBhc3N3b3JkU2FsdA==");
        // Invoke @PostConstruct init method
        Method init = PasswordUtil.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(passwordUtil);
    }

    @Test
    void encryptAndDecrypt_roundtrip_returnsOriginal() {
        String original = "mySecretPassword123";
        String encrypted = passwordUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = passwordUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_producesGcmPrefixedCiphertext() {
        String encrypted = passwordUtil.encrypt("test");
        assertTrue(encrypted.startsWith("v2:"), "New ciphertext must use GCM format with 'v2:' prefix");
    }

    @Test
    void encrypt_differentCallsProduceDifferentCiphertext() {
        // Each call uses a random IV, so two encryptions of the same plaintext differ
        String plain = "samePassword";
        String cipher1 = passwordUtil.encrypt(plain);
        String cipher2 = passwordUtil.encrypt(plain);
        assertNotEquals(cipher1, cipher2);
    }

    @Test
    void encryptAndDecrypt_emptyString_roundtrip() {
        String original = "";
        String encrypted = passwordUtil.encrypt(original);
        String decrypted = passwordUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encryptAndDecrypt_specialChars_roundtrip() {
        String original = "p@$$w0rd!#%^&*()";
        assertEquals(original, passwordUtil.decrypt(passwordUtil.encrypt(original)));
    }

    @Test
    void generateKey_returnsNonEmptyBase64String() {
        String key = PasswordUtil.generateKey();
        assertNotNull(key);
        assertFalse(key.isBlank());
        // A 128-bit AES key base64-encoded is 24 characters
        assertTrue(key.length() >= 20);
    }

    /**
     * 向后兼容测试：确保旧的CBC格式密文仍然可以被正确解密
     */
    @Test
    void decrypt_legacyCbcFormat_backwardCompatible() throws Exception {
        // 使用CBC模式手动加密，模拟旧版历史数据
        byte[] keyBytes = Base64.getDecoder().decode("VGVzdFBhc3N3b3JkU2FsdA==");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(iv));
        byte[] encryptedBytes = cipher.doFinal("legacyPassword".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] combined = new byte[16 + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, 16);
        System.arraycopy(encryptedBytes, 0, combined, 16, encryptedBytes.length);
        String cbcCipherText = Base64.getEncoder().encodeToString(combined);

        // 确保旧格式（无v2:前缀）仍可解密
        assertFalse(cbcCipherText.startsWith("v2:"));
        String decrypted = passwordUtil.decrypt(cbcCipherText);
        assertEquals("legacyPassword", decrypted);
    }
}
