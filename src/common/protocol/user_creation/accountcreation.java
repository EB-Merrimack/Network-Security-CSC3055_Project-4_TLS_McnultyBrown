package common.protocol.user_creation;

import merrimackutil.codec.Base32;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import common.protocol.messages.StatusMessage;
import common.protocol.user_auth.UserDatabase;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.Mac;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;


public class AccountCreation {

    // Simulated database (in a real-world scenario, this could be a database connection)
    private static Map<String, User> userDatabase = new HashMap<>();
    
    public static class User {
        private String username;
        private String passwordHash; // Store password securely using hash
        private String publicKey;
        private String totpKey; // Base32 encoded TOTP key
        private String privateKey; // The private key generated for the user
        
        // Constructor
        public User(String username, String passwordHash, String publicKey, String totpKey, String privateKey) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.publicKey = publicKey;
            this.totpKey = totpKey;
            this.privateKey = privateKey;
        }
    }

    // Method to create a new user account
    public static StatusMessage createAccount(String username, String password, String publicKey) {
        try {
            // Check if user already exists
            if (UserDatabase.containsKey(username)) {
                return new StatusMessage(false, "User already exists.");
            }
    
            // Generate salt
            byte[] saltBytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(saltBytes);
            String salt = Base64.getEncoder().encodeToString(saltBytes);
    
            // Hash password with PBKDF2 and salt
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 10000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            String passwordHash = Base64.getEncoder().encodeToString(hash);
    
            // Generate TOTP key
            byte[] totpBytes = new byte[20]; // 160-bit secret
            random.nextBytes(totpBytes);
            String totpKey = Base64.getEncoder().encodeToString(totpBytes);
    
            // Create User object
            common.protocol.user_auth.User user = new common.protocol.user_auth.User(
                salt,
                passwordHash,
                totpKey,
                username,
                publicKey
            );
    
            // Save user to database (writes to users.json)
            UserDatabase.put(username, user);
            
    
            // Respond with base64 TOTP key
            return new StatusMessage(true, totpKey);
    
        } catch (Exception e) {
            e.printStackTrace();
            return new StatusMessage(false, "Unexpected error.");
        }
    }

   

    
}
