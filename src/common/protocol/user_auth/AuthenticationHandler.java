package common.protocol.user_auth;

import common.protocol.messages.AuthenticateMessage;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public class AuthenticationHandler {

    public static boolean authenticate(AuthenticateMessage message) {
        try {
            String username = message.getUser();
            String password = message.getPass();
            String otp = message.getOtp();

           
            // 1. Check if user exists
            if (!UserDatabase.check(username)) {
                return false;
            }

            User user = UserDatabase.get(username);


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


            // Timing-safe comparison of hashes to avoid timing attacks
            if (!MessageDigest.isEqual(passwordHash.getBytes(), user.getPasswordHash().getBytes())) {
                return false;
            }

            // 3. Validate OTP using TOTP from the User object
            boolean otpValid = verifyTOTP(user.getTotpKey(), otp);

           

            return otpValid;

        } catch (Exception e) {
           
            e.printStackTrace();
            return false;
        }
    }

    private static boolean verifyTOTP(String base64Secret, String otp) {
        try {
           
    
            // Decode from Base64 (this matches what's stored in the users.json file)
            byte[] key = Base64.getDecoder().decode(base64Secret);
    
            long timeIndex = Instant.now().getEpochSecond() / 30;
    
    
            // Try ±3 time steps to account for clock skew
            for (int i = -3; i <= 3; i++) {
                String candidate = TOTP.generateTOTP(key, timeIndex + i);
    
    
                if (candidate.equals(otp)) {
                    return true;
                }
            }
    
            return false;
    
        } catch (Exception e) {
            return false;
        }
    }
}
