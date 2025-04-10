package common.protocol.user_creation;

import merrimackutil.codec.Base32;
import common.protocol.messages.StatusMessage;
import common.protocol.user_auth.UserDatabase;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;


public class CreateAccount {

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

    public static StatusMessage createAccount(String username, String password, String publicKey, String userfile) {
        try {
            // Load the user database from the file before doing anything else
            UserDatabase.load(userfile);
    
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
            String totpKey = Base32.encodeToString(totpBytes, true);
    
            // Create User object
            common.protocol.user_auth.User user = new common.protocol.user_auth.User(
                salt,
                passwordHash,
                totpKey,
                username,
                publicKey
            );
    
            // Save user to database and write back to file
            UserDatabase.put(username, user);
            UserDatabase.save(userfile);
    
            // Respond with base64 TOTP key
            return new StatusMessage(true, totpKey);
    
        } catch (Exception e) {
            e.printStackTrace();
            return new StatusMessage(false, "Unexpected error.");
        }
    }
    
   

    
}
