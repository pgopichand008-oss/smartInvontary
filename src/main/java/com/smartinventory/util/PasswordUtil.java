package com.smartinventory.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Security utility for hashing and verifying passwords using BCrypt.
 */
public final class PasswordUtil {

    private static final int BCRYPT_WORK_FACTOR = 12;

    private PasswordUtil() {
    }

    /**
     * Hashes a plaintext password using BCrypt with an adaptive salt work factor.
     *
     * @param plainTextPassword plaintext password
     * @return BCrypt hashed string
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty for hashing.");
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    /**
     * Checks if a plaintext password matches a stored BCrypt hash.
     *
     * @param plainTextPassword candidate plaintext password
     * @param hashedPassword    stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // In case of malformed hash
            return false;
        }
    }
}
