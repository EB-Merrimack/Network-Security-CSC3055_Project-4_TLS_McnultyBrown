package common.protocol.messages;

import common.protocol.Message;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

public class PubKeyRequest implements Message {
    private String user;

    public PubKeyRequest() {}

    public PubKeyRequest(String user) {
        this.user = user;
    }

    /**
     * Returns the username associated with this public key request.
     * @return the username associated with this public key request
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
        return "PubKeyRequest";
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "PubKeyRequest");
        obj.put("user", user);
        return obj;
    }

    /**
     * Deserialize a JSON object into a PubKeyRequest instance.
     *
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
     * Decodes a JSON object into a PubKeyRequest instance.
     *
     * @param obj the JSON object to decode, expected to be a JSONObject.
     * @return a deserialized PubKeyRequest instance.
     * @throws InvalidObjectException if the object is not a JSONObject or
     *                                if the "user" field is missing.
     */
    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        String user = obj.getString("user");
        return new PubKeyRequest(user);
    }

    /**
     * Returns a string representation of the PubKeyRequest object.
     * @return a string representation of the object in the format "[PubKeyRequest] user=<user>".
     */

    @Override
    public String toString() {
        return "[PubKeyRequest] user=" + user;
    }
}
