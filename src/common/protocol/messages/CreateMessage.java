package common.protocol.messages;

import merrimackutil.json.*;
import merrimackutil.json.types.*;

import java.io.InvalidObjectException;

public class CreateMessage implements JSONSerializable {
    private String user;
    private String pass;
    private String pubkey;

    public CreateMessage(String user, String pass, String pubkey) {
        this.user = user;
        this.pass = pass;
        this.pubkey = pubkey;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) throw new InvalidObjectException("Expected JSONObject");
        JSONObject json = (JSONObject) obj;

        this.user = json.getString("user");
        this.pass = json.getString("pass");
        this.pubkey = json.getString("pubkey");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "Create");
        obj.put("user", user);
        obj.put("pass", pass);
        obj.put("pubkey", pubkey);
        return obj;
    }
}