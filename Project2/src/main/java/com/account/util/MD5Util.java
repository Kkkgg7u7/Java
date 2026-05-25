package com.account.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {

    private MD5Util() {
    }

    public static String encrypt(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    public static String encryptWithSalt(String input, String salt) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return encrypt(input + salt);
    }

    public static boolean verify(String input, String encrypted) {
        if (input == null || encrypted == null) {
            return false;
        }
        return encrypt(input).equalsIgnoreCase(encrypted);
    }

    public static boolean verifyWithSalt(String input, String salt, String encrypted) {
        if (input == null || encrypted == null) {
            return false;
        }
        return encryptWithSalt(input, salt).equalsIgnoreCase(encrypted);
    }

}
