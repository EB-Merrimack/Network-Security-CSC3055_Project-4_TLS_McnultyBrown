package common.protocol.user_creation;

import common.protocol.messages.StatusMessage;
import common.protocol.user_auth.UserDatabase;

import org.bouncycastle.crypto.generators.SCrypt;

import java.security.SecureRandom;
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

    /**
     * Creates a new user in the user database given a username, password, and public key.
     * 
     * @param username the username of the new user
     * @param password the password of the new user
     * @param publicKey the encoded public key of the new user
     * @param userfile the file to load the user database from
     * @return a StatusMessage with a boolean indicating success or failure and a message
     *         containing the base64 encoded TOTP key if successful, or an error message
     *         otherwise
     */
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
    
            
            byte[] hash = SCrypt.generate(password.getBytes(), saltBytes, 2048, 8, 1, 16);
            String passwordHash = Base64.getEncoder().encodeToString(hash);


            // Generate TOTP key
           byte[] totpKeyBytes = new byte[64]; // 256-bit key (or 64 bytes for extra security)
            random.nextBytes(totpKeyBytes);
            String totpKey = Base64.getEncoder().encodeToString(totpKeyBytes);
            
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
