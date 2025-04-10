package common.protocol.user_auth;

import common.protocol.messages.AuthenticateMessage;
import merrimackutil.codec.Base32;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.Base64;

public class AuthenticationHandler {

    private static final boolean DEBUG = true;  // Set to false in production

    public static boolean authenticate(AuthenticateMessage message) {
        try {
            String username = message.getUser();
            String password = message.getPass();
            String otp = message.getOtp();

            if (DEBUG) {
                System.out.println("[DEBUG] AuthenticateRequest - user: " + username + ", password: " + password + ", otp: " + otp);
            }

            // 1. Check if user exists
            if (!UserDatabase.check(username)) {
                if (DEBUG) System.out.println("[DEBUG] User not found: " + username);
                return false;
            }

            User user = UserDatabase.get(username);

            if (DEBUG) {
                System.out.println("[DEBUG] Found user - user: " + user.getUser() + ", totpKey: " + user.getTotpKey());
            }

            // 2. Validate password hash
            byte[] saltBytes = Base64.getDecoder().decode(user.getSalt());
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 10000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            String passwordHash = Base64.getEncoder().encodeToString(hash);

            if (DEBUG) {
                System.out.println("[DEBUG] Validating password hash - generated hash: " + passwordHash);
            }

            // Timing-safe comparison of hashes to avoid timing attacks
            if (!MessageDigest.isEqual(passwordHash.getBytes(), user.getPasswordHash().getBytes())) {
                if (DEBUG) System.out.println("[DEBUG] Password hash mismatch.");
                return false;
            }

            // 3. Validate OTP using TOTP from the User object
            boolean otpValid = verifyTOTP(user.getTotpKey(), otp);

            if (DEBUG) {
                System.out.println("[DEBUG] OTP validation result: " + otpValid);
            }

            return otpValid;

        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("[DEBUG] Authentication failed due to exception: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    private static boolean verifyTOTP(String base32Secret, String otp) {
        try {
            if (DEBUG) {
                System.out.println("[DEBUG] Verifying OTP with TOTP key: " + base32Secret);
            }

            byte[] key = Base32.decode(base32Secret);

            long timeIndex = Instant.now().getEpochSecond() / 30;

            if (DEBUG) {
                System.out.println("[DEBUG] Time index for TOTP: " + timeIndex);
            }

            // Try timeIndex - 1, timeIndex, timeIndex + 1 for minor time skew
            for (int i = -3; i <= 3; i++) {
                String candidate = TOTP.generateTOTP(key, timeIndex + i);

                if (DEBUG) {
                    System.out.println("[DEBUG] OTP candidate for timeIndex + " + i + ": " + candidate);
                }

                if (candidate.equals(otp)) {
                    if (DEBUG) {
                        System.out.println("[DEBUG] OTP match found: " + otp);
                    }
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("[DEBUG] OTP verification failed due to exception: " + e.getMessage());
            }
            return false;
        }
    }
}
