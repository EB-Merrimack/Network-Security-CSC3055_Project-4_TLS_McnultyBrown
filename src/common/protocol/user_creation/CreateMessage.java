package common.protocol.user_creation;

import merrimackutil.json.types.*;
import common.protocol.Message;

import java.io.InvalidObjectException;

public class CreateMessage implements Message {
    private String user;
    private String pass;
    private String pubkey;

    public CreateMessage() {
        
    }

    public CreateMessage(String user, String pass, String pubkey) {
        this.user = user;
        this.pass = pass;
        this.pubkey = pubkey;
        System.out.println("[DEBUG] CreateMessage constructor called with user=" + user + ", pass=" + pass + ", pubkey=" + pubkey);
    }

    /**
     * Returns the username associated with this create message.
     *
     * @return the username associated with this create message
     */
    public String getUsername() {
        return user;
    }
    
    /**
     * Returns the user's password as a string.
     *
     * @return the user's password
     */
    public String getPassword() {
        return pass;
    }
    
    /**
     * Returns the user's public key as a Base64-encoded string.
     *
     * @return the user's public key
     */
    public String getPublicKey() {
        return pubkey;
    }

    /**
     * Deserialize a JSON object into a CreateMessage instance.
     *
     * @param obj the JSON object to deserialize
     * @throws InvalidObjectException if the object is not a JSONObject or
     *                                if the "user", "pass", or "pubkey"
     *                                fields are missing.
     */
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

    /**
     * Converts the object to a JSON type.
     *
     * @return a JSON type either JSONObject or JSONArray.
     * The returned JSONObject contains the type, user, pass, and pubkey fields.
     * The type field is a string with the value "Create".
     * The user field is a string with the value of the username.
     * The pass field is a string with the value of the password.
     * The pubkey field is a string with the value of the public key.
     */
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

    /**
     * Gets the type of this message.
     *
     * @return the type of this message, as a string, which is "Create"
     */
    @Override
    public String getType() {
        System.out.println("[DEBUG] getType() called, returning 'Create'");
        return "Create";
    }

    /**
     * Decodes a JSON object into a CreateMessage instance.
     *
     * @param obj the JSON object to decode
     * @return a deserialized CreateMessage instance
     * @throws InvalidObjectException if the object is not a valid JSONObject
     */
    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        System.out.println("[DEBUG] decode() called with JSONObject: " + obj.toString());
        String user = obj.getString("user");
        String pass = obj.getString("pass");
        String pubkey = obj.getString("pubkey");
        System.out.println("[DEBUG] Decoding CreateMessage with user=" + user + ", pass=" + pass + ", pubkey=" + pubkey);
        return new CreateMessage(user, pass, pubkey);
    }
}
