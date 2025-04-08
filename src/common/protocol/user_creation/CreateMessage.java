package common.protocol.user_creation;

import merrimackutil.json.types.*;
import common.protocol.Message;

import java.io.InvalidObjectException;

public class CreateMessage implements Message {
    private String user;
    private String pass;
    private String pubkey;

    public CreateMessage(String user, String pass, String pubkey) {
        this.user = user;
        this.pass = pass;
        this.pubkey = pubkey;
        System.out.println("[DEBUG] CreateMessage constructor called with user=" + user + ", pass=" + pass + ", pubkey=" + pubkey);
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        System.out.println("[DEBUG] deserialize() called");
        if (!(obj instanceof JSONObject)) {
            System.out.println("[ERROR] Expected JSONObject, got " + obj.getClass().getSimpleName());
            throw new InvalidObjectException("Expected JSONObject");
        }

        JSONObject json = (JSONObject) obj;

        this.user = json.getString("user");
        this.pass = json.getString("pass");
        this.pubkey = json.getString("pubkey");

        System.out.println("[DEBUG] Deserialized CreateMessage: user=" + user + ", pass=" + pass + ", pubkey=" + pubkey);
    }

    @Override
    public JSONType toJSONType() {
        System.out.println("[DEBUG] toJSONType() called");
        JSONObject obj = new JSONObject();
        obj.put("type", "Create");
        obj.put("user", user);
        obj.put("pass", pass);
        obj.put("pubkey", pubkey);
        System.out.println("[DEBUG] Serialized CreateMessage: " + obj.toString());
        return obj;
    }

    @Override
    public String getType() {
        System.out.println("[DEBUG] getType() called");
        return "Create";
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        System.out.println("[DEBUG] decode() called with JSONObject: " + obj.toString());
        String user = obj.getString("user");
        String pass = obj.getString("pass");
        String pubkey = obj.getString("pubkey");
        System.out.println("[DEBUG] Decoding CreateMessage with user=" + user + ", pass=" + pass + ", pubkey=" + pubkey);
        return new CreateMessage(user, pass, pubkey);
    }

    @Override
    public String getUsername() {
        return user;
    }
}
