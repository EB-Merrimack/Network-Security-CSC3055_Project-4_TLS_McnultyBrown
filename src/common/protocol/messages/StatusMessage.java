package common.protocol.messages;

import merrimackutil.json.*;
import merrimackutil.json.types.*;

import java.io.InvalidObjectException;

public class StatusMessage implements JSONSerializable {
    private boolean status;
    private String payload;

    public StatusMessage() {}
    public StatusMessage(boolean status, String payload) {
        this.status = status;
        this.payload = payload;
    }

    public boolean getStatus() { return status; }
    public String getPayload() { return payload; }

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
}