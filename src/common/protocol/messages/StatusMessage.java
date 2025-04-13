package common.protocol.messages;

import merrimackutil.json.types.*;

import java.io.InvalidObjectException;

import common.protocol.Message;

public class StatusMessage implements Message {
    private boolean status;
    private static String payload;

    public StatusMessage() {}
    @SuppressWarnings("static-access")
    public StatusMessage(boolean status, String payload) {
        this.status = status;
        this.payload = payload;
    }

    public boolean getStatus() { return status; }
    public static String getPayload() { return payload; }

    /**
     * Deserialize a JSON object into a StatusMessage instance.
     *
     * @param obj the JSON object to deserialize
     * @throws InvalidObjectException if the object is not a JSONObject or if
     *                                "status" or "payload" fields are missing.
     */
    @SuppressWarnings("static-access")
    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) throw new InvalidObjectException("Expected JSONObject");
        JSONObject json = (JSONObject) obj;

        this.status = json.getBoolean("status");
        this.payload = json.getString("payload");
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     * The returned JSONObject contains the type, status, and payload fields.
     * The type field is a string with the value "Status".
     * The status field is a boolean with the value of the status of the server.
     * The payload field is a string with the payload message from the server.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "Status");
        obj.put("status", status);
        obj.put("payload", payload);
        return obj;
    }
/**
 * Gets the message type as a string.
 * @return the message type as a string, which is "Status".
 */

    @Override
    public String getType() {
        return "Status";
    }

    /**
     * Decodes a JSON object into a StatusMessage instance.
     * @param obj the JSON object to decode, expected to be a JSONObject.
     * @return a deserialized StatusMessage instance.
     * @throws InvalidObjectException if the object is not a JSONObject or
     *                                if the "status" or "payload" fields are missing.
     */
    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        StatusMessage msg = new StatusMessage();
        msg.deserialize(obj);
        return msg;
    }
}