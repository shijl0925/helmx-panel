package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void encryptAndDecrypt_roundtrip_returnsOriginal() {
        String original = "mySecretPassword123";
        String encrypted = PasswordUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = PasswordUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_differentCallsProduceDifferentCiphertext() {
        // Each call uses a random IV, so two encryptions of the same plaintext differ
        String plain = "samePassword";
        String cipher1 = PasswordUtil.encrypt(plain);
        String cipher2 = PasswordUtil.encrypt(plain);
        assertNotEquals(cipher1, cipher2);
    }

    @Test
    void encryptAndDecrypt_emptyString_roundtrip() {
        String original = "";
        String encrypted = PasswordUtil.encrypt(original);
        String decrypted = PasswordUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encryptAndDecrypt_specialChars_roundtrip() {
        String original = "p@$$w0rd!#%^&*()";
        assertEquals(original, PasswordUtil.decrypt(PasswordUtil.encrypt(original)));
    }

    @Test
    void generateKey_returnsNonEmptyBase64String() {
        String key = PasswordUtil.generateKey();
        assertNotNull(key);
        assertFalse(key.isBlank());
        // A 128-bit AES key base64-encoded is 24 characters
        assertTrue(key.length() >= 20);
    }
}
