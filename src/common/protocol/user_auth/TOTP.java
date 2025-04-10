package common.protocol.user_auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.time.Instant;

public class TOTP {

    private static final int OTP_LENGTH = 6;
    private static final int TIME_STEP = 30;  // 30 seconds time step

    /**
     * Generates a TOTP based on a secret and time index.
     *
     * @param secret The base32-encoded secret key
     * @param timeIndex The time step index (e.g., number of 30-second intervals since Unix epoch)
     * @return The OTP as a string of digits
     */
    public static String generateTOTP(byte[] secret, long timeIndex) {
        try {
            // Convert the time index to byte[] (8 bytes for long)
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(timeIndex);
            byte[] timeBytes = buffer.array();

            // HMAC-SHA1 generation
            Mac hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(secret, "HmacSHA1");
            hmac.init(keySpec);
            byte[] hash = hmac.doFinal(timeBytes);

            // Truncate the hash to get the OTP
            int offset = hash[19] & 0xF;  // Last byte's low nibble
            int binary = ((hash[offset] & 0x7f) << 24) |
                         ((hash[offset + 1] & 0xff) << 16) |
                         ((hash[offset + 2] & 0xff) << 8) |
                         (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, OTP_LENGTH);

            // Pad OTP to the desired length
            return String.format("%0" + OTP_LENGTH + "d", otp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifies a TOTP against a given OTP.
     *
     * @param secret The base32-encoded secret key
     * @param otp The OTP to verify
     * @return true if OTP is valid, false otherwise
     */
    public static boolean verifyTOTP(byte[] secret, String otp) {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP;
        System.out.println("Current Time Index: " + timeIndex);
        for (int i = -1; i <= 1; i++) {
            String generatedOtp = generateTOTP(secret, timeIndex + i);
            System.out.println("Generated OTP for T+" + i + ": " + generatedOtp);
            if (generatedOtp != null && generatedOtp.equals(otp)) {
                return true;
            }
        }
        return false;    
    }
 

 
}
