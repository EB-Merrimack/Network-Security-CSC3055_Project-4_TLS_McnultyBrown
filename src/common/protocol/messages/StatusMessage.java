package common.protocol.messages;

import merrimackutil.json.types.*;

import java.io.InvalidObjectException;

import common.protocol.Message;

public class StatusMessage implements Message {
    private boolean status;
    private static String payload;

    public StatusMessage() {}
    public StatusMessage(boolean status, String payload) {
        this.status = status;
        this.payload = payload;
    }

    public boolean getStatus() { return status; }
    public static String getPayload() { return payload; }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) throw new InvalidObjectException("Expected JSONObject");
        JSONObject json = (JSONObject) obj;

        this.status = json.getBoolean("status");
        this.payload = json.getString("payload");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "Status");
        obj.put("status", status);
        obj.put("payload", payload);
        return obj;
    }
    @Override
    public String getType() {
        return "Status";
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        StatusMessage msg = new StatusMessage();
        msg.deserialize(obj);
        return msg;
    }
}