package com.tizhen.core.llm;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 选手 API Key 的 AES-256-GCM 加密。密文格式为 Base64(IV || ciphertext || tag)，
 * 明文只允许在单次调用栈内存在。
 */
@Component
public class LlmCredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;
    private final int keyVersion;

    public LlmCredentialCipher(LlmProperties properties) {
        this.keyVersion = Math.max(properties.getCredentialKeyVersion(), 1);
        byte[] keyBytes = parseKey(properties.getCredentialEncryptionKey());
        this.key = keyBytes == null ? null : new SecretKeySpec(keyBytes, "AES");
    }

    public boolean isConfigured() {
        return key != null;
    }

    public int keyVersion() {
        return keyVersion;
    }

    public String encrypt(String plaintext) {
        if (key == null) {
            throw new CredentialCipherException("LLM 凭证加密密钥未配置");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new CredentialCipherException("LLM 凭证加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        if (key == null) {
            throw new CredentialCipherException("LLM 凭证加密密钥未配置");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            if (payload.length <= IV_LENGTH) {
                throw new CredentialCipherException("LLM 凭据格式非法");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new CredentialCipherException("LLM 凭据无法解密");
        }
    }

    private byte[] parseKey(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        String trimmed = configured.trim();
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException exception) {
            bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            return null;
        }
        return bytes;
    }

    public static class CredentialCipherException extends RuntimeException {
        public CredentialCipherException(String message) {
            super(message);
        }
    }
}
