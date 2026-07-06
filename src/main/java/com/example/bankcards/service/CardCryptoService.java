package com.example.bankcards.service;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CardCryptoService {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private final String secret;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec keySpec;

    public CardCryptoService(@Value("${card.crypto.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(secret) || secret.length() < 16) {
            throw new IllegalStateException("CARD_CRYPTO_SECRET must contain at least 16 characters");
        }
        this.keySpec = new SecretKeySpec(sha256(secret), "AES");
    }

    public String normalize(String cardNumber) {
        String normalized = NON_DIGIT.matcher(cardNumber == null ? "" : cardNumber).replaceAll("");
        if (normalized.length() != 16) {
            throw new IllegalArgumentException("Card number must contain exactly 16 digits");
        }
        return normalized;
    }

    public String encrypt(String normalizedNumber) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(normalizedNumber.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt card number", exception);
        }
    }

    public String hash(String normalizedNumber) {
        return HexFormat.of().formatHex(sha256(normalizedNumber));
    }

    public String lastFourDigits(String normalizedNumber) {
        return normalizedNumber.substring(normalizedNumber.length() - 4);
    }

    public String mask(String lastFourDigits) {
        return "**** **** **** " + lastFourDigits;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
