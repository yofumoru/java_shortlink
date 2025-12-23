package org.example.Shortlink.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

public class ShortCodeGenerator {

    public static String generate(UUID userId, String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = userId + url + System.nanoTime();

            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash)
                    .substring(0, 7);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации shortCode", e);
        }
    }
}