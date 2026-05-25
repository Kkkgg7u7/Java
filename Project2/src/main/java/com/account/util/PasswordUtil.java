package com.account.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    private PasswordUtil() {
    }

    public static String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }
        return ENCODER.encode(rawPassword);
    }

    public static boolean verify(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        if (isLegacyMd5(storedPassword)) {
            return MD5Util.verify(rawPassword, storedPassword);
        }
        return false;
    }

    public static boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !isBcrypt(storedPassword);
    }

    private static boolean isBcrypt(String storedPassword) {
        return storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$");
    }

    private static boolean isLegacyMd5(String storedPassword) {
        return storedPassword.matches("^[a-fA-F0-9]{32}$");
    }
}
