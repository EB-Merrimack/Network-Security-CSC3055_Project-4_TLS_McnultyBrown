package common.protocol.user_auth;

import common.protocol.messages.AuthenticateMessage;
import merrimackutil.codec.Base32;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.Base64;

public class AuthenticationHandler {

    public static boolean authenticate(AuthenticateMessage message) {
        try {
            String username = message.getUser();
            String password = message.getPass();
            String otp = message.getOtp();

            // 1. Check if user exists
            if (!UserDatabase.containsKey(username)) {
                return false;
            }

            User user = UserDatabase.get(username);

            // 2. Validate password hash
            byte[] saltBytes = Base64.getDecoder().decode(user.getSalt());
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 10000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            String passwordHash = Base64.getEncoder().encodeToString(hash);

            if (!passwordHash.equals(user.getPasswordHash())) {
                return false;
            }

            // 3. Validate OTP using TOTP from the User object
            return verifyTOTP(user.getTotpKey(), otp);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean verifyTOTP(String base32Secret, String otp) {
        try {
            // Use the TOTP key directly from the User's stored `totpKey`
            byte[] key = Base32.decode(base32Secret);

            long timeIndex = Instant.now().getEpochSecond() / 30;
            for (int i = -1; i <= 1; i++) {
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
