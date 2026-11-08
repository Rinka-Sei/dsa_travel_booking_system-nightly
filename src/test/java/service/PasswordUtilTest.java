package service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying salted SHA-256 password hashing, constant-time verification,
 * and legacy fallback handling in {@link PasswordUtil}.
 */
public class PasswordUtilTest {

    @Test
    @DisplayName("hashPassword generates salted hash formatted with delimiter")
    public void testHashPasswordFormat() {
        String hash = PasswordUtil.hashPassword("secret123");
        assertNotNull(hash);
        assertTrue(hash.contains("$"), "Hash must contain '$' separating salt and hash");
        String[] parts = hash.split("\\$");
        assertEquals(2, parts.length);
        assertEquals(32, parts[0].length(), "16-byte salt in hex should be 32 characters");
        assertEquals(64, parts[1].length(), "SHA-256 digest in hex should be 64 characters");
    }

    @Test
    @DisplayName("verifyPassword validates correct password and rejects wrong password")
    public void testVerifySaltedPassword() {
        String password = "mySecurePassword!99";
        String storedHash = PasswordUtil.hashPassword(password);

        assertTrue(PasswordUtil.verifyPassword(password, storedHash), "Correct password should verify successfully");
        assertFalse(PasswordUtil.verifyPassword("wrongPassword", storedHash), "Incorrect password must be rejected");
        assertFalse(PasswordUtil.verifyPassword("", storedHash), "Empty password must be rejected");
        assertFalse(PasswordUtil.verifyPassword(null, storedHash), "Null password must be rejected");
        assertFalse(PasswordUtil.verifyPassword(password, null), "Null stored hash must be rejected");
    }

    @Test
    @DisplayName("Consecutive hashes of identical password produce distinct salts and digests")
    public void testSaltRandomness() {
        String password = "samePassword123";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertNotEquals(hash1, hash2, "Two salted hashes of the same password must have different salts");
        assertTrue(PasswordUtil.verifyPassword(password, hash1));
        assertTrue(PasswordUtil.verifyPassword(password, hash2));
    }

    @Test
    @DisplayName("verifyPassword supports legacy unhashed plain-text records for backward compatibility")
    public void testLegacyPlainTextVerification() {
        String legacyAdminPass = "admin123";
        assertTrue(PasswordUtil.verifyPassword("admin123", legacyAdminPass),
                "Legacy plain-text passwords must verify directly for seed compatibility");
        assertFalse(PasswordUtil.verifyPassword("wrongPass", legacyAdminPass),
                "Incorrect candidate password against plain-text record must be rejected");
    }

    @Test
    @DisplayName("verifyPassword supports legacy unsalted SHA-256 hex digest")
    public void testLegacyUnsaltedSha256Verification() {
        String plain = "admin123";
        String rawSha256 = PasswordUtil.computeSha256(plain);

        assertTrue(PasswordUtil.verifyPassword(plain, rawSha256),
                "Unsalted SHA-256 hash should verify correctly");
        assertFalse(PasswordUtil.verifyPassword("wrongPass", rawSha256));
    }

    @Test
    @DisplayName("hashPassword rejects null or blank inputs")
    public void testInvalidPasswordInputs() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword("   "));
    }
}
