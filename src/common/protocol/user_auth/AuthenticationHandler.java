package common.protocol.user_auth;

import common.protocol.messages.AuthenticateMessage;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public class AuthenticationHandler {

    private static final boolean DEBUG = false;  // Set to false in production

    /**
     * Authenticates a user using an AuthenticateMessage.
     * 
     * 1. Checks if the user exists.
     * 2. Validates the password hash using SCrypt.
     * 3. Validates the OTP using TOTP from the User object.
     * 
     * @param message the AuthenticateMessage to process
     * @return true if the user is authenticated; otherwise, false.
     * @throws Exception if an error occurs during authentication
     */
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
            byte[] hash = org.bouncycastle.crypto.generators.SCrypt.generate(
                password.getBytes(),       // password from input
                saltBytes,                 // stored salt
                2048,                      // cost
                8,                         // block size
                1,                         // parallelization
                16                         // key length (128 bits)
            );
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

    /**
     * Verifies a given OTP using the TOTP algorithm and a given Base64-encoded secret key.
     * 
     * @param base64Secret the Base64-encoded secret key
     * @param otp the one-time password to verify
     * @return true if the OTP is valid, false otherwise
     * @throws Exception if an error occurs during verification
     */
    private static boolean verifyTOTP(String base64Secret, String otp) {
        try {
            if (DEBUG) {
                System.out.println("[DEBUG] Verifying OTP with TOTP key (Base64): " + base64Secret);
            }
    
            // 🔥 Decode from Base64 (this matches what's stored in the users.json file)
            byte[] key = Base64.getDecoder().decode(base64Secret);
    
            long timeIndex = Instant.now().getEpochSecond() / 30;
    
            if (DEBUG) {
                System.out.println("[DEBUG] Time index for TOTP: " + timeIndex);
            }
    
            // Try ±3 time steps to account for clock skew
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
