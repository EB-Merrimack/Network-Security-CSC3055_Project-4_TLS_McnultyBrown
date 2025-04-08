package common.protocol.user_auth;

import merrimackutil.json.*;
import merrimackutil.json.types.*;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

public class UserDatabase {
    private static String usersFilePath = null;
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
    public static void setFilePath(String path) {
        usersFilePath = path;
    }

    static {
        loadUsers();
    }

    public static boolean containsKey(String username) {
        return userMap.containsKey(username);
    }

    public static void put(String username, User newUser) {
        userMap.put(username, newUser);
        saveUsers();
    }

    private static void loadUsers() {
        try {
            File file = new File(usersFilePath);
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
                JSONType entryType = (JSONType) entries.get(i);                if (!(entryType instanceof JSONObject)) continue;

                User user = new User();
                user.deserialize(entryType); // ✅ this works because entryType is a JSONType
                userMap.put(user.getUser(), user);
            }

            System.out.println("[UserDatabase] Loaded " + userMap.size() + " users.");
        } catch (Exception e) {
            System.err.println("[UserDatabase] Error loading users.json: " + e.getMessage());
        }
    }

    private static void saveUsers() {
        try {
            JSONArray entries = new JSONArray();
            for (User user : userMap.values()) {
                entries.add(user.toJSONType());
            }

            UserDBWrapper db = new UserDBWrapper(entries);
            JsonIO.writeSerializedObject(db, new File(usersFilePath));
            System.out.println("[UserDatabase] Saved users to file.");
        } catch (IOException e) {
            System.err.println("[UserDatabase] Failed to save users: " + e.getMessage());
        }
    }
}