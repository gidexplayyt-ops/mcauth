package com.gidexplayyt.mcauth.core;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

public class GoogleAuthenticatorUtil {
    private static final int SECRET_SIZE = 10;
    private static final String CRYPTO = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;

    public static String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        new SecureRandom().nextBytes(buffer);
        Base32 codec = new Base32();
        return codec.encodeToString(buffer).replace("=", "");
    }

    public static String getCode(String secret) {
        try {
            Base32 codec = new Base32();
            byte[] decodedKey = codec.decode(secret);
            long timeWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
            byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();
            SecretKeySpec signKey = new SecretKeySpec(decodedKey, CRYPTO);
            Mac mac = Mac.getInstance(CRYPTO);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int code = truncatedHash % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%06d", code);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Не удалось получить код Google Authenticator", e);
        }
    }
}
