package common.protocol.user_creation;

import common.protocol.Message;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JsonIO;

public class UserMessage implements Message {
    private String user;
    private String password;
    private String publicKey;

    // Constructor to initialize fields
    public UserMessage(String user, String password, String publicKey) {
        this.user = user;
        this.password = password;
        this.publicKey = publicKey;
    }

    // Getter and Setter methods
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    // Override toString() to easily print the message (useful for debugging)
    @Override
    public String toString() {
        return "UserMessage{" +
                "user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }

    // Implement the inherited abstract methods

    @Override
    public String getUsername() {
        return this.user;
    }

    @Override
    public String getType() {
        return "UserMessage";
    }

    @Override
    public Message decode(JSONObject json) {
        // Deserialize the JSON into UserMessage fields
        String user = json.getString("user");
        String password = json.getString("password");
        String publicKey = json.getString("publicKey");
        
        return (Message) new UserMessage(user, password, publicKey);
    }

    @Override
    public JSONObject toJSONType() {
        // Convert this UserMessage into a JSONObject
        JSONObject json = new JSONObject();
        json.put("user", this.user);
        json.put("password", this.password);
        json.put("publicKey", this.publicKey);
        return json;
    }

    @Override
    public void deserialize(JSONType jsonType) {
        // Deserialize JSONType into the UserMessage object fields
        if (jsonType instanceof JSONObject) {
            JSONObject json = (JSONObject) jsonType;
            this.user = json.getString("user");
            this.password = json.getString("password");
            this.publicKey = json.getString("publicKey");
        } else {
            throw new IllegalArgumentException("Invalid JSON type for deserialization");
        }
    }
}
