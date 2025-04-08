package common.protocol.user_auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class UserDatabase {

    // Thread-safe map to store users
    private static final Map<String, User> users = new ConcurrentHashMap<>();

    public static boolean containsKey(String username) {
        return users.containsKey(username);
    }

    public static void put(String username, User newUser) {
        users.put(username, newUser);
    }

    public static User get(String username) {
        return users.get(username);
    }

    public static void clear() {
        users.clear();
    }
}
