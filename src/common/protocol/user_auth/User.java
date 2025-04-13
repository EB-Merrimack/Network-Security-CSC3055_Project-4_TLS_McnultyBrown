package common.protocol.user_auth;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;
import java.util.Base64;

public class User implements JSONSerializable {
    private String salt;
    private String pass;
    private String totpKey;
    private String user;
    private static String pubkey;

    public User() {}

    public User(String salt, String pass, String totpKey, String user, String pubkey) {
        this.salt = salt;
        this.pass = pass;
        this.totpKey = totpKey;
        this.user = user;
        this.pubkey = pubkey;
    }

    public String getSalt() { return salt; }
    public String getPass() { return pass; }
    public String getPasswordHash() { return pass; } // ✅ Implemented
    public String getTotpKey() { return totpKey; }
    public String getUser() { return user; }
    public static String getPubkey() { return pubkey; }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Expected a JSON object for user.");
        }
        JSONObject json = (JSONObject) obj;
        this.salt = json.getString("salt");
        this.pass = json.getString("pass");
        this.totpKey = json.getString("totp-key");
        this.user = json.getString("user");
        this.pubkey = json.getString("pubkey");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("salt", salt);
        json.put("pass", pass);
        json.put("totp-key", totpKey);
        json.put("user", user);
        json.put("pubkey", pubkey);
        return json;
    }
}
