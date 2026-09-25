package com.airquality.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for hashing and verifying passwords securely using Core Java standard libraries
 * (SHA-256 with cryptographically secure random salt) without requiring third-party JARs.
 */
public class PasswordUtil {

    private static final int SALT_BYTES = 16;

    /**
     * Generates a random 16-byte salt, hashes the plain password with SHA-256,
     * and returns the combined salt and hash in the format: Base64(salt) + ":" + Base64(hash)
     */
    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes("UTF-8"));

            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);

            return saltBase64 + ":" + hashBase64;
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verifies if the provided plain password matches the stored salted hash.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            if (storedHash == null || !storedHash.contains(":")) {
                return false;
            }
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] actualHash = md.digest(password.getBytes("UTF-8"));

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }
}
