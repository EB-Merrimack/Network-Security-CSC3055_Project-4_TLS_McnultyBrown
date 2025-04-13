package common.protocol.messages;

import common.protocol.Message;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;

import java.io.InvalidObjectException;

public class GetMessage implements Message {
    private String user;

    public GetMessage() {}

    public GetMessage(String user) {
        this.user = user;
    }

    /**
     * Returns the username to get posts from.
     * @return the username.
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the message type as a string.
     * @return the message type as a string.
     */
    @Override
    public String getType() {
        return "GetMessage";
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "GetMessage");
        obj.put("user", user);
        return obj;
    }

    /**
     * Deserialize a JSON object into a GetMessage instance.
     * @param obj the JSON object to deserialize
     * @throws InvalidObjectException if the object is not a JSON object
     */
    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject.");
        }
        JSONObject json = (JSONObject) obj;
        this.user = json.getString("user");
    }

/**
 * Decodes a JSON object into a GetMessage instance.
 *
 * @param obj the JSON object to decode
 * @return a GetMessage instance
 * @throws InvalidObjectException if the object is not a valid JSONObject
 */

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        return new GetMessage(obj.getString("user"));
    }

    /**
     * Converts the object to a string representation.
     * @return a string representation of the object in the format "[GetMessage] user=<user>".
     */
    @Override
    public String toString() {
        return "[GetMessage] user=" + user;
    }
}