package common.protocol.user_auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;

public class TOTP {

    private static final int OTP_LENGTH = 6;
    private static final int TIME_STEP = 30;  // Time step in seconds (TOTP standard is 30 seconds)

    /**
     * Generates a TOTP based on a secret and time index.
     *
     * @param secret The Base32-decoded secret key
     * @param timeIndex The time step index (number of 30-second intervals since Unix epoch)
     * @return The OTP as a string of digits
     */
    public static String generateTOTP(byte[] secret, long timeIndex) {
        try {
            // Convert the time index (long) to a byte array (8 bytes)
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(timeIndex);
            byte[] timeBytes = buffer.array();

            // HMAC-SHA1 generation using the secret as the key
            Mac hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(secret, "HmacSHA1");
            hmac.init(keySpec);
            byte[] hash = hmac.doFinal(timeBytes);

            // Dynamic Truncation to extract the OTP
            int offset = hash[19] & 0xF;  // Use the last byte's low nibble to determine the offset
            int binary = ((hash[offset] & 0x7f) << 24) |
                         ((hash[offset + 1] & 0xff) << 16) |
                         ((hash[offset + 2] & 0xff) << 8) |
                         (hash[offset + 3] & 0xff);

            // Modulo operation to ensure OTP length (e.g., 6 digits)
            int otp = binary % (int) Math.pow(10, OTP_LENGTH);

            // Pad OTP to the desired length
            return String.format("%0" + OTP_LENGTH + "d", otp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

 

 
}
