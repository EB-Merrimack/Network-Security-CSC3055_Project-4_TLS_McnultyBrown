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

    /**
     * Returns the username associated with this post.
     * @return the username associated with this post
     */
    public String getUser() { 
        return user; 
    }

/**
 * Retrieves the message content of the post.
 * 
 * @return the encrypted message as a String.
 */

    /**
     * Retrieves the message content of the post.
     * 
     * @return the encrypted message as a String.
     */
    public String getMessage() { 
        return message; 
    }

    /**
     * Returns the wrapped key, which is the session key encrypted with the server's RSA key.
     * @return the wrapped key as a Base64-encoded string
     */
    public String getWrappedKey() { 
        return wrappedKey; 
    }

    /**
     * Returns the initialization vector used for AES encryption.
     * @return the initialization vector as a Base64-encoded string
     */
    public String getIv() { 
        return iv; 
    }

    /**
     * Gets the type of the post. This is used to determine which type of post it is (e.g. regular post, delete post, etc.)
     * @return the type of the post as a string
     */
    public String getType() {
        return type;  // Getter for the type
    }

    // Convert this Post to a PostMessage
    public PostMessage toPostMessage() {
        return new PostMessage(user, message, wrappedKey, iv);
    }

    /**
     * Deserializes a JSON object into a Post instance.
     * @param obj the JSON object to deserialize, expected to be a JSONObject.
     * @throws InvalidObjectException if the object is not a JSONObject or if required fields are missing
     */
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

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     * The returned JSONObject contains the type, message, wrappedkey, user, and iv fields.
     */
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
