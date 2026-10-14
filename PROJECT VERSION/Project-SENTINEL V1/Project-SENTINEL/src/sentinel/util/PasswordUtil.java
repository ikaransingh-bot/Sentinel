package sentinel.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hasht en controleert wachtwoorden met PBKDF2 (HMAC-SHA256).
 * Opslagformaat: iteraties:base64(salt):base64(hash)
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {

        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            byte[] hash = createHash(password, salt, ITERATIONS);

            return ITERATIONS
                    + ":" + Base64.getEncoder().encodeToString(salt)
                    + ":" + Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Password kon niet worden gehashed.");
        }
    }

    /** Geeft alleen true als het wachtwoord bij de opgeslagen hash past. */
    public static boolean verifyPassword(String password, String storedHash) {

        try {
            String[] parts = storedHash.split(":");

            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            byte[] actualHash = createHash(password, salt, iterations);

            // Vergelijken in constante tijd.
            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (Exception e) {
            // Een ongeldige opgeslagen hash telt als fout wachtwoord.
            return false;
        }
    }

    private static byte[] createHash(String password, byte[] salt, int iterations) throws Exception {

        PBEKeySpec specification = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        return factory.generateSecret(specification).getEncoded();
    }
}
