package common.protocol.post;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;

import java.io.InvalidObjectException;

import common.protocol.messages.PostMessage;

/**
 * Represents a single encrypted post on the board.
 */
public class Post implements JSONSerializable {
    private String user;
    private String message;
    private String wrappedKey;
    private String iv;
    private String type;  // New field for the type of the post

    // Constructor with type
    public Post(String user, String message, String wrappedKey, String iv, String type) {
        this.user = user;
        this.message = message;
        this.wrappedKey = wrappedKey;
        this.iv = iv;
        this.type = type;
    }

    // Constructor that takes a JSONObject and initializes the object
    public Post(JSONObject obj) throws InvalidObjectException {
        deserialize(obj);
    }

    public String getUser() { 
        return user; 
    }

/**
 * Retrieves the message content of the post.
 * 
 * @return the encrypted message as a String.
 */

    public String getMessage() { 
        return message; 
    }

    public String getWrappedKey() { 
        return wrappedKey; 
    }

    public String getIv() { 
        return iv; 
    }

    public String getType() {
        return type;  // Getter for the type
    }

    // Convert this Post to a PostMessage
    public PostMessage toPostMessage() {
        return new PostMessage(user, message, wrappedKey, iv);
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Post expects a JSONObject.");
        }

        JSONObject postObj = (JSONObject) obj;
        postObj.checkValidity(new String[]{"user", "message", "wrappedkey", "iv", "type"});  // Include type field check

        this.user = postObj.getString("user");
        this.message = postObj.getString("message");
        this.wrappedKey = postObj.getString("wrappedkey");
        this.iv = postObj.getString("iv");
        this.type = postObj.getString("type");  // Deserialize type field
    }

    @Override
    public JSONType toJSONType() {
        JSONObject postObj = new JSONObject();
        postObj.put("type", "Post");  // Include the type field in the JSON serialization
        postObj.put("message", message);  // Place message field after type
        postObj.put("wrappedkey", wrappedKey);  // Place wrappedkey field
        postObj.put("user", user);  // Place user field after wrappedkey
        postObj.put("iv", iv);  // Place iv field last
        return postObj;
    }
}
