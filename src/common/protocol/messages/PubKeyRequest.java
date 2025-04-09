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

    public String getUser() {
        return user;
    }

    @Override
    public String getType() {
        return "PubKeyRequest";
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "PubKeyRequest");
        obj.put("user", user);
        return obj;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject.");
        }

        JSONObject json = (JSONObject) obj;
        this.user = json.getString("user");
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        String user = obj.getString("user");
        return new PubKeyRequest(user);
    }

    @Override
    public String toString() {
        return "[PubKeyRequest] user=" + user;
    }
}
