package common.protocol.user_creation;

import merrimackutil.codec.Base32;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import common.protocol.user_auth.User;
import common.protocol.user_auth.UserDatabase;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AccountCreation {

  

    // Method to create a new user account
    public static String createAccount(String username, String password, String publicKey) {
        // Check if the username already exists in the database
        if (UserDatabase.containsKey(username)) {
            return generateResponse(false, "User already exists.");
        }

        // Hash the password securely using PBKDF2
        String passwordHash = hashPassword(password);
        
        // Generate a real TOTP key using Bouncy Castle
        String totpKey = generateTOTPKey();
        
        // Generate the RSA key pair (private/public)
        String privateKey = generatePrivateKey();
        
        // Create the user object
        User newUser = new User(username, passwordHash, publicKey, totpKey, privateKey);
        
        // Store the user in the "database"
        UserDatabase.put(username, newUser);

        // Return the status message with the base64 encoded TOTP key (payload)
        return generateResponse(true, totpKey);
    }

    // Hash the password using PBKDF2
    private static String hashPassword(String password) {
        try {
            // PBKDF2 hashing
            char[] chars = password.toCharArray();
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            KeySpec spec = new PBEKeySpec(chars, salt, 10000, 256);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = f.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);  // Base64 encoded password hash
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Generate a real TOTP key using Bouncy Castle
    private static String generateTOTPKey() {
        try {
            // Generate a random 128-bit key (16 bytes)
            SecureRandom random = new SecureRandom();
            byte[] key = new byte[16];
            random.nextBytes(key);

            // HMAC SHA1 for generating TOTP
            HMac hmac = new HMac(new SHA1Digest());
            hmac.init(new KeyParameter(key));

            // Example data (can be modified to include the time-based component)
            byte[] data = new byte[8]; // The data for TOTP can be the current timestamp, etc.
            hmac.update(data, 0, data.length);

            // Generate the TOTP value
            byte[] result = new byte[hmac.getMacSize()];
            hmac.doFinal(result, 0);

            // Convert the TOTP result to Base32 (without padding)
            return Base32.encodeToString(result, false).replace("=", "");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Generate RSA private/public key pair
    private static String generatePrivateKey() {
        try {
            // Generate RSA Key Pair
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            
            // Extract the private key
            PrivateKey privateKey = keyPair.getPrivate();
            
            // Encode the private key to base64
            return Base64.getEncoder().encodeToString(privateKey.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Method to generate the response message
    private static String generateResponse(boolean status, String payload) {
        // If status is true, the payload will be the base64 encoded TOTP key
        if (status) {
            // Encode the TOTP key in Base32 (without padding)
            String base32TOTPKey = Base32.encodeToString(payload.getBytes(), status).replace("=", "");
            return generateStatusMessage(status, base32TOTPKey);
        }
        
        // If status is false, send an error message as the payload
        return generateStatusMessage(status, payload);
    }

    // Helper method to generate a status response
    private static String generateStatusMessage(boolean status, String payload) {
        // Simulate the JSON response (In a real system, this would be structured as a JSON object)
        return "{ \"status\": " + status + ", \"payload\": \"" + payload + "\" }";
    }


}
