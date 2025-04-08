package common.protocol.messages;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;

import java.io.InvalidObjectException;

/**
 * Represents a single encrypted post on the board.
 */
public class Post implements JSONSerializable {
    private String user;
    private String message;
    private String wrappedKey;
    private String iv;

    public Post(String user, String message, String wrappedKey, String iv) {
        this.user = user;
        this.message = message;
        this.wrappedKey = wrappedKey;
        this.iv = iv;
    }

    public Post(JSONObject obj) throws InvalidObjectException {
        deserialize(obj);
    }

    public String getUser() { return user; }
    public String getMessage() { return message; }
    public String getWrappedKey() { return wrappedKey; }
    public String getIv() { return iv; }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Post expects a JSONObject.");
        }

        JSONObject postObj = (JSONObject) obj;
        postObj.checkValidity(new String[]{"user", "message", "wrappedkey", "iv"});

        this.user = postObj.getString("user");
        this.message = postObj.getString("message");
        this.wrappedKey = postObj.getString("wrappedkey");
        this.iv = postObj.getString("iv");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject postObj = new JSONObject();
        postObj.put("type", "Post");
        postObj.put("user", user);
        postObj.put("message", message);
        postObj.put("wrappedkey", wrappedKey);
        postObj.put("iv", iv);
        return postObj;
    }
}
