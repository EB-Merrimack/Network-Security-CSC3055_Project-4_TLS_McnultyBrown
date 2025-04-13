package common.protocol.user_auth;

import merrimackutil.json.*;
import merrimackutil.json.types.*;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

public class UserDatabase {
    private static Map<String, User> userMap = new HashMap<>();

    // Wrapper class to serialize root-level "entries" array
    private static class UserDBWrapper implements JSONSerializable {
        private final JSONArray entries;

        public UserDBWrapper(JSONArray entries) {
            this.entries = entries;
        }

        @Override
        public JSONType toJSONType() {
            JSONObject root = new JSONObject();
            root.put("entries", entries);
            return root;
        }

        @Override
        public void deserialize(JSONType obj) {
            // unused
        }
    }

    public static boolean containsKey(String username) {
        return userMap.containsKey(username);
    }

    public static void put(String username, User newUser) {
        userMap.put(username, newUser);
    }

    private static void loadUsers(String userfile) {
        try {
            File file = new File(userfile);
            if (!file.exists()) {
                System.out.println("[UserDatabase] users.json not found. Starting fresh.");
                return;
            }

            JSONType raw = JsonIO.readObject(file);
            if (!(raw instanceof JSONObject)) {
                throw new InvalidObjectException("users.json is not a valid JSON object.");
            }

            JSONObject root = (JSONObject) raw;
            JSONArray entries = root.getArray("entries");

            for (int i = 0; i < entries.size(); i++) {
                JSONType entryType = (JSONType) entries.get(i);
                if (!(entryType instanceof JSONObject)) continue;

                User user = new User();
                user.deserialize(entryType); // ✅ this works because entryType is a JSONType
                userMap.put(user.getUser(), user);
            }

            System.out.println("[UserDatabase] Loaded " + userMap.size() + " users.");
        } catch (Exception e) {
            System.err.println("[UserDatabase] Error loading users.json: " + e.getMessage());
        }
    }

    private static void saveUsers(String userfile) {
        try {
            JSONArray entries = new JSONArray();
            for (User user : userMap.values()) {
                entries.add(user.toJSONType());
            }

            UserDBWrapper db = new UserDBWrapper(entries);
            JsonIO.writeFormattedObject(db, new File(userfile));
            System.out.println("[UserDatabase] Saved users to file.");
        } catch (IOException e) {
            System.err.println("[UserDatabase] Failed to save users: " + e.getMessage());
        }
    }

    // Method to load users from a custom userfile
    public static void load(String userfile) {
        loadUsers(userfile); // Delegate to the existing loadUsers method
    }

    public static void save(String userfile) {
        saveUsers(userfile); // Delegate to the existing saveUsers method
    }

    public static User get(String username) {
        return userMap.get(username);
    }

    public static String getPubkey(String username) {
        return userMap.get(username).getPubkey();
    }

    // Check method to verify if the username exists in the database
    public static boolean check(String username) {
        Object userfile = server.Configuration.getUsersFile();
        // Ensure userMap is loaded from the file
        if (userfile == null || userMap.isEmpty()) {
            System.out.println("[UserDatabase] Loading users from file: " + userfile);
            loadUsers((String) userfile);
        }

        // Debugging: log the check process
        System.out.println("[UserDatabase] Checking if user exists: " + username);

        if (userMap.containsKey(username)) {
            System.out.println("[UserDatabase] User " + username + " found.");
            return true;
        } else {
            System.out.println("[UserDatabase] User " + username + " not found.");
            return false;
        }
    }

    // Method to retrieve the encoded public key of a user
     // Method to retrieve the encoded public key of a user
     public static String getEncodedPublicKey(String username) {
        try {
            // Ensure userMap is populated
            Object userfile = server.Configuration.getUsersFile();
            if (userfile == null || userMap == null || userMap.isEmpty()) {
                System.out.println("[UserDatabase] Loading users from file: " + userfile);
                loadUsers((String) userfile); // Load users into userMap
            }

            // Check if user exists
            if (!userMap.containsKey(username)) {
                System.out.println("[UserDatabase] User not found: " + username);
                return null;
            }

           

            // Get the Base64-encoded public key
            String pubkey = UserDatabase.getPubkey(username);

            if (pubkey == null || pubkey.isEmpty()) {
                System.out.println("[UserDatabase] Public key for user " + username + " is not available.");
                return null;
            }

            // Return the Base64-encoded public key string
            return pubkey;

        } catch (Exception e) {
            System.out.println("[UserDatabase] Error reading user public key: " + e.getMessage());
            return null;
        }
    }

}
