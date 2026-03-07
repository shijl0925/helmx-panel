package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    @Test
    void init_missingSecretKey_throwsIllegalStateException() throws Exception {
        PasswordUtil util = new PasswordUtil();
        Field field = PasswordUtil.class.getDeclaredField("secretKeyValue");
        field.setAccessible(true);
        field.set(util, "");

        Method init = PasswordUtil.class.getDeclaredMethod("init");
        init.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> init.invoke(util));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }
}
