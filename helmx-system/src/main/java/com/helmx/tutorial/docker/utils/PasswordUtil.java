package com.helmx.tutorial.docker.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PasswordUtil {
    // AES密钥长度
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    // 使用固定密钥，实际应用中应该从配置文件或环境变量中读取
    private static final String SECRET_KEY = "bDAORZ9t7/+1RpyV5JIXJg==";

    /**
     * 加密密码
     * @param plainText 明文密码
     * @return 加密后的密码
     */
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("encrypt failed", e);
        }
    }

    /**
     * 解密密码
     * @param cipherText 加密后的密码
     * @return 明文密码
     */
    public static String decrypt(String cipherText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decryptedBytes);
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
